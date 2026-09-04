package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ContentPart;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * EvoLink 채팅 API로 영상을 분석하는 어댑터. 영상은 바이트로 내려받지 않고 S3 presigned GET URL을
 * video_url로 넘겨 모델이 직접 읽게 한다. 그래서 URL이 유효한 동안(발급 후 1시간)만 분석할 수 있다.
 * 사용자 메시지는 반드시 비디오를 텍스트보다 먼저 둔다. 순서가 뒤집히면 모델이 지시(언어·출력 형식)를
 * 무시하고 영어 장면 묘사로 빠지는 경향이 있다.
 * 모델 호출·응답 해석 실패는 VideoAnalysisFailedException으로 번역해 던지고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
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
	// 영상 입력 시 모델이 지시를 무시하고 영어 장면 묘사로 빠지는 경향이 있어, 지시는 system 메시지로 분리해
	// 한국어와 항목 구조를 규칙과 출력 형식으로 강제한다. 내용은 resources/prompts/video-analysis 참고
	private final String systemPrompt;
	private final String userPrompt;
	// AiReportContent 구조의 JSON Schema를 strict로 강제해, 응답이 항상 스키마에 맞는 순수 JSON임을 API가 보장한다
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
		String content = completeOrThrow(videoUrl);

		AiReportContent reportContent = parseContent(content, videoUrl);
		return objectMapper.writeValueAsString(reportContent);
	}

	// HTTP 오류와 쓸 수 없는 응답을 경계에서 도메인 예외로 번역한다. 그 외 예외는 버그로 보고 그대로 둔다
	private String completeOrThrow(String videoUrl) {
		try {
			return chatClient.complete(buildRequest(videoUrl));
		}
		catch (RestClientException | EvoLinkResponseException e) {
			throw new VideoAnalysisFailedException("영상 분석 모델 호출에 실패했습니다: " + withoutQuery(videoUrl), e);
		}
	}

	private EvoLinkChatRequest buildRequest(String videoUrl) {
		return new EvoLinkChatRequest(
				properties.model(),
				List.of(
						ChatMessage.system(systemPrompt),
						ChatMessage.user(List.of(
								ContentPart.videoUrl(videoUrl, properties.videoFps()),
								ContentPart.text(renderUserPrompt())))),
				responseFormat,
				properties.maxTokens(),
				properties.temperature(),
				new Thinking(properties.thinking()));
	}

	// json_schema strict가 순수 JSON을 보장하므로 응답을 바로 파싱한다. 어긋나면 예외로 드러낸다
	private AiReportContent parseContent(String rawText, String videoUrl) {
		AiReportContent content;
		try {
			content = objectMapper.readValue(rawText, AiReportContent.class);
		}
		catch (JacksonException e) {
			throw new VideoAnalysisFailedException("영상 분석 응답이 JSON 형식이 아닙니다: " + withoutQuery(videoUrl), e);
		}
		// 스키마는 필드 구조만 강제하고 빈 배열은 막지 못하므로 핵심 항목 유무는 여기서 검증한다
		if (content.report().isEmpty()) {
			throw new VideoAnalysisFailedException("영상 분석 응답에 report 항목이 없습니다: " + withoutQuery(videoUrl));
		}
		return content;
	}

	// presigned URL의 쿼리에는 서명이 들어 있어 예외 메시지(로그)에 남기지 않는다
	private static String withoutQuery(String url) {
		int queryStart = url.indexOf('?');
		return queryStart < 0 ? url : url.substring(0, queryStart);
	}

	// 교육 목록은 기동 시점이 아니라 호출마다 조회한다. 영상 분석은 저빈도 작업이라 쿼리 비용이 무시 가능하고,
	// 토픽이 바뀌어도 재기동 없이 반영된다.
	private String renderUserPrompt() {
		String topics = topicFinder.findAllOrdered().stream()
				.map(topic -> topic.title() + ": " + topic.description())
				.collect(Collectors.joining("\n"));
		return userPrompt.replace(TOPICS_PLACEHOLDER, topics);
	}
}
