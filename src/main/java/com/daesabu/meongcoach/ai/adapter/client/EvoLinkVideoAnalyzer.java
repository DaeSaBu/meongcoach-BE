package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest.ContentPart;
import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * EvoLink 채팅 API로 영상을 분석하는 어댑터. 영상은 바이트로 내려받지 않고 S3 presigned GET URL을
 * video_url로 넘겨 모델이 직접 읽게 한다. 그래서 URL이 유효한 동안(발급 후 1시간)만 분석할 수 있다.
 * 사용자 메시지는 반드시 비디오를 텍스트보다 먼저 둔다. 순서가 뒤집히면 모델이 지시(언어·출력 형식)를
 * 무시하고 영어 장면 묘사로 빠지는 경향이 있다.
 * 실패는 그대로 전파하고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
public class EvoLinkVideoAnalyzer implements VideoAnalyzer {

	private static final String SYSTEM_PROMPT_PATH = "prompts/video-analysis/system.md";
	private static final String USER_PROMPT_PATH = "prompts/video-analysis/user.md";
	private static final String TOPICS_PLACEHOLDER = "{{topics}}";

	private final EvoLinkChatClient chatClient;
	private final EvoLinkProperties properties;
	private final TopicFinder topicFinder;
	private final ObjectMapper objectMapper;
	// 영상 입력 시 모델이 지시를 무시하고 영어 장면 묘사로 빠지는 경향이 있어, 지시는 system 메시지로 분리해
	// 한국어와 항목 구조를 규칙과 출력 형식으로 강제한다. 내용은 resources/prompts/video-analysis 참고
	private final String systemPrompt;
	private final String userPrompt;

	public EvoLinkVideoAnalyzer(EvoLinkChatClient chatClient, EvoLinkProperties properties,
			TopicFinder topicFinder, ObjectMapper objectMapper) {
		this.chatClient = chatClient;
		this.properties = properties;
		this.topicFinder = topicFinder;
		this.objectMapper = objectMapper;
		this.systemPrompt = PromptLoader.load(SYSTEM_PROMPT_PATH);
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		if (!userPrompt.contains(TOPICS_PLACEHOLDER)) {
			throw new IllegalStateException(
					"사용자 프롬프트에 " + TOPICS_PLACEHOLDER + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	@Override
	public String analyze(String videoUrl) {
		String content = chatClient.complete(buildRequest(videoUrl));

		AiReportContent reportContent = parseContent(content, videoUrl);
		return objectMapper.writeValueAsString(reportContent);
	}

	private EvoLinkChatRequest buildRequest(String videoUrl) {
		return new EvoLinkChatRequest(
				properties.model(),
				List.of(
						ChatMessage.system(systemPrompt),
						ChatMessage.user(List.of(
								ContentPart.videoUrl(videoUrl, properties.videoFps()),
								ContentPart.text(renderUserPrompt())))),
				ResponseFormat.jsonObject(),
				properties.maxTokens(),
				properties.temperature(),
				new Thinking(properties.thinking()));
	}

	// json_object 형식을 지정해도 모델이 지시를 어기고 코드 펜스나 설명을 붙일 수 있어
	// 첫 '{'부터 마지막 '}'까지만 잘라 파싱한다
	private AiReportContent parseContent(String rawText, String videoUrl) {
		String json = extractJsonObject(rawText, videoUrl);
		AiReportContent content;
		try {
			content = objectMapper.readValue(json, AiReportContent.class);
		}
		catch (JacksonException e) {
			throw new IllegalStateException("영상 분석 응답이 JSON 형식이 아닙니다: " + withoutQuery(videoUrl), e);
		}
		if (content.report().isEmpty()) {
			throw new IllegalStateException("영상 분석 응답에 report 항목이 없습니다: " + withoutQuery(videoUrl));
		}
		return content;
	}

	private static String extractJsonObject(String rawText, String videoUrl) {
		int start = rawText.indexOf('{');
		int end = rawText.lastIndexOf('}');
		if (start < 0 || end < start) {
			throw new IllegalStateException("영상 분석 응답에서 JSON을 찾지 못했습니다: " + withoutQuery(videoUrl));
		}
		return rawText.substring(start, end + 1);
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
