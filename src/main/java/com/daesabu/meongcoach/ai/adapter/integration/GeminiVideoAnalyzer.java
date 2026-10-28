package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent.Recommend;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.File;
import com.google.genai.types.FileData;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.google.genai.types.RegisterFilesResponse;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.VideoMetadata;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class GeminiVideoAnalyzer implements VideoAnalyzer {

	private static final String SYSTEM_PROMPT_PATH = "prompts/video-analysis/system.md";
	private static final String USER_PROMPT_PATH = "prompts/video-analysis/user.md";
	private static final String SCHEMA_PATH = "prompts/video-analysis/schema.json";
	private static final String TOPICS_PLACEHOLDER = "{{topics}}";
	private static final String JSON_MIME_TYPE = "application/json";
	private static final int CANDIDATE_COUNT = 1;

	private final Client client;
	private final GeminiProperties properties;
	private final GoogleCredentials credentials;
	private final TopicFinder topicFinder;
	private final ObjectMapper objectMapper;
	private final String userPrompt;
	private final GenerateContentConfig generateConfig;

	@Autowired
	public GeminiVideoAnalyzer(GeminiProperties properties, GoogleCredentials credentials, TopicFinder topicFinder,
			ObjectMapper objectMapper) {
		this(newClient(properties), properties, credentials, topicFinder, objectMapper);
	}

	GeminiVideoAnalyzer(Client client, GeminiProperties properties, GoogleCredentials credentials,
			TopicFinder topicFinder, ObjectMapper objectMapper) {
		this.client = client;
		this.properties = properties;
		this.credentials = credentials;
		this.topicFinder = topicFinder;
		this.objectMapper = objectMapper;
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		this.generateConfig = GenerateContentConfig.builder()
				.systemInstruction(Content.fromParts(Part.fromText(PromptLoader.load(SYSTEM_PROMPT_PATH))))
				.responseMimeType(JSON_MIME_TYPE)
				.responseJsonSchema(objectMapper.readValue(PromptLoader.load(SCHEMA_PATH), Map.class))
				.candidateCount(CANDIDATE_COUNT)
				.maxOutputTokens(properties.maxOutputTokens())
				.temperature(properties.temperature())
				.thinkingConfig(ThinkingConfig.builder().thinkingLevel(properties.thinkingLevel()))
				.build();
		if (!userPrompt.contains(TOPICS_PLACEHOLDER)) {
			throw new IllegalStateException(
					"사용자 프롬프트에 " + TOPICS_PLACEHOLDER + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	private static Client newClient(GeminiProperties properties) {
		HttpOptions httpOptions = HttpOptions.builder()
				.timeout((int) properties.responseTimeout().toMillis())
				.build();
		return Client.builder()
				.apiKey(properties.apiKey())
				.httpOptions(httpOptions)
				.build();
	}

	@Override
	public String analyze(String videoUri) {
		List<TopicResult> topics = topicFinder.findAllOrdered();
		File video = registerOrThrow(videoUri);
		String content = generateOrThrow(video, topics, videoUri);

		AiReportContent reportContent = parseContent(content, topics, videoUri);
		return objectMapper.writeValueAsString(reportContent);
	}

	private File registerOrThrow(String videoUri) {
		RegisterFilesResponse response;
		try {
			response = client.files.registerFiles(credentials, List.of(videoUri), null);
		}
		catch (ApiException | GenAiIOException e) {
			throw new VideoAnalysisFailedException("영상 등록 호출에 실패했습니다: " + videoUri, e);
		}
		List<File> files = response.files().orElse(List.of());
		if (files.isEmpty() || files.getFirst().uri().isEmpty()) {
			throw new VideoAnalysisFailedException("영상 등록 응답에 파일 URI가 없습니다: " + videoUri);
		}
		return files.getFirst();
	}

	private String generateOrThrow(File video, List<TopicResult> topics, String videoUri) {
		GenerateContentResponse response;
		try {
			response = client.models.generateContent(properties.model(), userContent(video, topics), generateConfig);
		}
		catch (ApiException | GenAiIOException e) {
			throw new VideoAnalysisFailedException("영상 분석 모델 호출에 실패했습니다: " + videoUri, e);
		}
		return extractText(response, videoUri);
	}

	private Content userContent(File video, List<TopicResult> topics) {
		Part videoPart = Part.builder()
				.fileData(FileData.builder()
						.fileUri(video.uri().orElseThrow())
						.mimeType(video.mimeType().orElse(null)))
				.videoMetadata(VideoMetadata.builder().fps(properties.videoFps()))
				.build();
		return Content.fromParts(videoPart, Part.fromText(renderUserPrompt(topics)));
	}

	private static String extractText(GenerateContentResponse response, String videoUri) {
		List<Candidate> candidates = response.candidates().orElse(List.of());
		if (candidates.isEmpty()) {
			throw new VideoAnalysisFailedException("영상 분석 응답에 candidates가 없습니다: " + videoUri);
		}
		FinishReason finishReason = candidates.getFirst().finishReason()
				.orElse(new FinishReason(FinishReason.Known.FINISH_REASON_UNSPECIFIED));
		if (finishReason.knownEnum() == FinishReason.Known.MAX_TOKENS) {
			throw new VideoAnalysisFailedException("영상 분석 응답이 max_output_tokens 상한에 걸려 잘렸습니다: " + videoUri);
		}
		if (finishReason.knownEnum() != FinishReason.Known.STOP) {
			throw new VideoAnalysisFailedException(
					"영상 분석 응답이 정상 종료되지 않았습니다(" + finishReason + "): " + videoUri);
		}
		String text = response.text();
		if (text == null || text.isBlank()) {
			throw new VideoAnalysisFailedException("영상 분석 응답 내용이 비어 있습니다: " + videoUri);
		}
		return text;
	}

	private AiReportContent parseContent(String rawText, List<TopicResult> topics, String videoUri) {
		AiReportContent content;
		try {
			content = objectMapper.readValue(rawText, AiReportContent.class);
		}
		catch (JacksonException e) {
			throw new VideoAnalysisFailedException("영상 분석 응답이 JSON 형식이 아닙니다: " + videoUri, e);
		}
		if (content == null || content.report().isEmpty()) {
			throw new VideoAnalysisFailedException("영상 분석 응답에 report 항목이 없습니다: " + videoUri);
		}
		requireKnownTopicIds(content, topics, videoUri);
		return content;
	}

	private static void requireKnownTopicIds(AiReportContent content, List<TopicResult> topics, String videoUri) {
		Set<Long> knownIds = topics.stream()
				.map(TopicResult::id)
				.collect(Collectors.toCollection(HashSet::new));
		List<Long> unknownIds = content.recommend().stream()
				.map(Recommend::topicId)
				.filter(topicId -> !knownIds.contains(topicId))
				.toList();
		if (unknownIds.isEmpty()) {
			return;
		}
		throw new VideoAnalysisFailedException(
				"영상 분석 응답의 추천 교육 topicId가 교육 목록에 없습니다: " + unknownIds + ", " + videoUri);
	}

	private String renderUserPrompt(List<TopicResult> topics) {
		String topicLines = topics.stream()
				.map(GeminiVideoAnalyzer::renderTopicLine)
				.collect(Collectors.joining("\n"));
		return userPrompt.replace(TOPICS_PLACEHOLDER, topicLines);
	}

	private static String renderTopicLine(TopicResult topic) {
		return "- topicId: " + topic.id() + ", 교육 이름: " + topic.title() + ", 설명: " + topic.description();
	}
}
