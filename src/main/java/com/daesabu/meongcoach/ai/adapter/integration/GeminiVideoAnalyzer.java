package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent.Recommend;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent.ReportSection;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent.Solution;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.File;
import com.google.genai.types.FileData;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.RegisterFilesResponse;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.VideoMetadata;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class GeminiVideoAnalyzer implements VideoAnalyzer {

	private static final String SYSTEM_PROMPT_PATH = "prompts/video-analysis/system.md";
	private static final String USER_PROMPT_PATH = "prompts/video-analysis/user.md";
	private static final String SCHEMA_PATH = "prompts/video-analysis/schema.json";
	private static final String TOPICS_PLACEHOLDER = "{{topics}}";
	private static final String CONTEXT_PLACEHOLDER = "{{context}}";
	private static final String EMPTY_CONTEXT = "없음";
	private static final String JSON_MIME_TYPE = "application/json";
	private static final int CANDIDATE_COUNT = 1;

	private final Client client;
	private final GeminiProperties properties;
	private final GoogleCredentials credentials;
	private final TopicFinder topicFinder;
	private final ObjectMapper objectMapper;
	private final String userPrompt;
	private final GenerateContentConfig generateConfig;

	public GeminiVideoAnalyzer(Client client, GeminiProperties properties, GoogleCredentials credentials,
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
		requirePlaceholder(TOPICS_PLACEHOLDER);
		requirePlaceholder(CONTEXT_PLACEHOLDER);
	}

	private void requirePlaceholder(String placeholder) {
		if (!userPrompt.contains(placeholder)) {
			throw new IllegalStateException("사용자 프롬프트에 " + placeholder + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	@Override
	public VideoAnalysisResult analyze(String videoUri, String context) {
		List<TopicResult> topics = topicFinder.findAllOrdered();
		File video = registerOrThrow(videoUri);
		String prompt = renderUserPrompt(topics, context);
		String content = generateOrThrow(video, prompt, videoUri);

		GeneratedReport generated = parseGenerated(content, videoUri);
		AiReportContent reportContent = validateContent(generated.toContent(), topics, videoUri);
		String reportJson = objectMapper.writeValueAsString(reportContent);
		return new VideoAnalysisResult(generated.title(), reportJson);
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

	private String generateOrThrow(File video, String prompt, String videoUri) {
		GenerateContentResponse response;
		try {
			response = client.models.generateContent(properties.model(), userContent(video, prompt), generateConfig);
		}
		catch (ApiException | GenAiIOException e) {
			throw new VideoAnalysisFailedException("영상 분석 모델 호출에 실패했습니다: " + videoUri, e);
		}
		return extractText(response, videoUri);
	}

	private Content userContent(File video, String prompt) {
		Part videoPart = Part.builder()
				.fileData(FileData.builder()
						.fileUri(video.uri().orElseThrow())
						.mimeType(video.mimeType().orElse(null)))
				.videoMetadata(VideoMetadata.builder().fps(properties.videoFps()))
				.build();
		return Content.fromParts(videoPart, Part.fromText(prompt));
	}

	private static String extractText(GenerateContentResponse response, String videoUri) {
		try {
			return GeminiResponses.extractText(response);
		}
		catch (GeminiResponseException e) {
			throw new VideoAnalysisFailedException("영상 분석 " + e.getMessage() + ": " + videoUri, e);
		}
	}

	private GeneratedReport parseGenerated(String rawText, String videoUri) {
		GeneratedReport generated;
		try {
			generated = objectMapper.readValue(rawText, GeneratedReport.class);
		}
		catch (JacksonException e) {
			throw new VideoAnalysisFailedException("영상 분석 응답이 JSON 형식이 아닙니다: " + videoUri, e);
		}
		if (generated == null) {
			throw new VideoAnalysisFailedException("영상 분석 응답에 report 항목이 없습니다: " + videoUri);
		}
		return generated;
	}

	private static AiReportContent validateContent(AiReportContent content, List<TopicResult> topics,
			String videoUri) {
		if (content.report().isEmpty()) {
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

	private String renderUserPrompt(List<TopicResult> topics, String context) {
		String topicLines = topics.stream()
				.map(GeminiVideoAnalyzer::renderTopicLine)
				.collect(Collectors.joining("\n"));
		return userPrompt.replace(TOPICS_PLACEHOLDER, topicLines)
				.replace(CONTEXT_PLACEHOLDER, renderContext(context));
	}

	private static String renderContext(String context) {
		if (context == null || context.isBlank()) {
			return EMPTY_CONTEXT;
		}
		return context.strip();
	}

	private static String renderTopicLine(TopicResult topic) {
		return "- topicId: " + topic.id() + ", 교육 이름: " + topic.title() + ", 설명: " + topic.description();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeneratedReport(
			String title,
			List<Recommend> recommend,
			List<ReportSection> report,
			List<Solution> solution) {

		AiReportContent toContent() {
			return new AiReportContent(recommend, report, solution);
		}
	}
}
