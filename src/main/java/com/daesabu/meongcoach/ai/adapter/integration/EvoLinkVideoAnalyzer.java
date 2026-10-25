package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent.Recommend;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.shared.integration.evolink.EvoLinkChatClient;
import com.daesabu.meongcoach.shared.integration.evolink.EvoLinkResponseException;
import com.daesabu.meongcoach.shared.integration.evolink.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.shared.integration.evolink.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.shared.integration.evolink.dto.EvoLinkChatRequest.ContentPart;
import com.daesabu.meongcoach.shared.integration.evolink.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.shared.integration.evolink.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EvoLinkVideoAnalyzer implements VideoAnalyzer {

	private static final String SYSTEM_PROMPT_PATH = "prompts/video-analysis/system.md";
	private static final String USER_PROMPT_PATH = "prompts/video-analysis/user.md";
	private static final String SCHEMA_PATH = "prompts/video-analysis/schema.json";
	private static final String TOPICS_PLACEHOLDER = "{{topics}}";

	private final EvoLinkChatClient chatClient;
	private final EvoLinkProperties properties;
	private final TopicFinder topicFinder;
	private final ObjectMapper objectMapper;
	private final String systemPrompt;
	private final String userPrompt;
	private final ResponseFormat responseFormat;

	public EvoLinkVideoAnalyzer(EvoLinkChatClient chatClient, EvoLinkProperties properties,
			TopicFinder topicFinder, ObjectMapper objectMapper) {
		this.chatClient = chatClient;
		this.properties = properties;
		this.topicFinder = topicFinder;
		this.objectMapper = objectMapper;
		this.systemPrompt = PromptLoader.load(SYSTEM_PROMPT_PATH);
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		this.responseFormat = ResponseFormat.jsonSchema("ai_report",
				objectMapper.readValue(PromptLoader.load(SCHEMA_PATH), Map.class));
		if (!userPrompt.contains(TOPICS_PLACEHOLDER)) {
			throw new IllegalStateException(
					"사용자 프롬프트에 " + TOPICS_PLACEHOLDER + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	@Override
	public String analyze(String videoUrl) {
		List<TopicResult> topics = topicFinder.findAllOrdered();
		String content = completeOrThrow(videoUrl, topics);

		AiReportContent reportContent = parseContent(content, topics, videoUrl);
		return objectMapper.writeValueAsString(reportContent);
	}

	private String completeOrThrow(String videoUrl, List<TopicResult> topics) {
		try {
			EvoLinkChatRequest request = buildRequest(videoUrl, topics);
			return chatClient.complete(request);
		}
		catch (RestClientException | EvoLinkResponseException e) {
			throw new VideoAnalysisFailedException("영상 분석 모델 호출에 실패했습니다: " + withoutQuery(videoUrl), e);
		}
	}

	private EvoLinkChatRequest buildRequest(String videoUrl, List<TopicResult> topics) {
		return new EvoLinkChatRequest(
				properties.model(),
				List.of(
						ChatMessage.system(systemPrompt),
						ChatMessage.user(List.of(
								ContentPart.videoUrl(videoUrl, properties.videoFps()),
								ContentPart.text(renderUserPrompt(topics))))),
				responseFormat,
				properties.maxTokens(),
				properties.temperature(),
				new Thinking(properties.thinking()));
	}

	private AiReportContent parseContent(String rawText, List<TopicResult> topics, String videoUrl) {
		AiReportContent content;
		try {
			content = objectMapper.readValue(rawText, AiReportContent.class);
		}
		catch (JacksonException e) {
			throw new VideoAnalysisFailedException("영상 분석 응답이 JSON 형식이 아닙니다: " + withoutQuery(videoUrl), e);
		}
		if (content == null || content.report().isEmpty()) {
			throw new VideoAnalysisFailedException("영상 분석 응답에 report 항목이 없습니다: " + withoutQuery(videoUrl));
		}
		requireKnownTopicIds(content, topics, videoUrl);
		return content;
	}

	private static void requireKnownTopicIds(AiReportContent content, List<TopicResult> topics, String videoUrl) {
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
				"영상 분석 응답의 추천 교육 topicId가 교육 목록에 없습니다: " + unknownIds + ", " + withoutQuery(videoUrl));
	}

	private static String withoutQuery(String url) {
		int queryStart = url.indexOf('?');
		return queryStart < 0 ? url : url.substring(0, queryStart);
	}

	private String renderUserPrompt(List<TopicResult> topics) {
		String topicLines = topics.stream()
				.map(EvoLinkVideoAnalyzer::renderTopicLine)
				.collect(Collectors.joining("\n"));
		return userPrompt.replace(TOPICS_PLACEHOLDER, topicLines);
	}

	private static String renderTopicLine(TopicResult topic) {
		return "- topicId: " + topic.id() + ", 교육 이름: " + topic.title() + ", 설명: " + topic.description();
	}
}
