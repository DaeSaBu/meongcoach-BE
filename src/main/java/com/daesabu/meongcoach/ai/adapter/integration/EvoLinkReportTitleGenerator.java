package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * EvoLink 채팅 API로 리포트 제목을 생성하는 어댑터. 영상을 다시 분석하지 않고 이미 만든 리포트 JSON
 * 텍스트만 넘겨 한 줄 제목을 받는다. 응답은 json_schema strict로 {"title": ...} 형태를 강제하므로
 * 코드 펜스·따옴표 같은 평문 정제 없이 파싱만 한다.
 * 모델 호출·응답 해석 실패는 ReportTitleGenerationFailedException으로 번역해 던지고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
public class EvoLinkReportTitleGenerator implements ReportTitleGenerator {

	private static final String SYSTEM_PROMPT_PATH = "prompts/report-title/system.md";
	private static final String USER_PROMPT_PATH = "prompts/report-title/user.md";
	private static final String SCHEMA_PATH = "prompts/report-title/schema.json";
	private static final String REPORT_PLACEHOLDER = "{{report}}";

	private final EvoLinkChatClient chatClient;
	private final EvoLinkProperties properties;
	private final ObjectMapper objectMapper;
	private final String systemPrompt;
	private final String userPrompt;
	private final ResponseFormat responseFormat;

	public EvoLinkReportTitleGenerator(EvoLinkChatClient chatClient, EvoLinkProperties properties,
			ObjectMapper objectMapper) {
		this.chatClient = chatClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.systemPrompt = PromptLoader.load(SYSTEM_PROMPT_PATH);
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		this.responseFormat = ResponseFormat.jsonSchema("report_title",
				objectMapper.readValue(PromptLoader.load(SCHEMA_PATH), Map.class));
		if (!userPrompt.contains(REPORT_PLACEHOLDER)) {
			throw new IllegalStateException(
					"사용자 프롬프트에 " + REPORT_PLACEHOLDER + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	@Override
	public String generateTitle(String reportContentJson) {
		String content = completeOrThrow(reportContentJson);

		String title = parseTitle(content);
		if (title.isBlank()) {
			throw new ReportTitleGenerationFailedException("리포트 제목 생성 결과가 비어 있습니다");
		}
		return title;
	}

	// HTTP 오류와 쓸 수 없는 응답을 경계에서 도메인 예외로 번역한다. 그 외 예외는 버그로 보고 그대로 둔다
	private String completeOrThrow(String reportContentJson) {
		try {
			return chatClient.complete(buildRequest(reportContentJson));
		}
		catch (RestClientException | EvoLinkResponseException e) {
			throw new ReportTitleGenerationFailedException("리포트 제목 생성 모델 호출에 실패했습니다", e);
		}
	}

	private EvoLinkChatRequest buildRequest(String reportContentJson) {
		return new EvoLinkChatRequest(
				properties.model(),
				List.of(
						ChatMessage.system(systemPrompt),
						ChatMessage.user(userPrompt.replace(REPORT_PLACEHOLDER, reportContentJson))),
				responseFormat,
				properties.maxTokens(),
				properties.temperature(),
				new Thinking(properties.thinking()));
	}

	// 길이 등 저장 규칙의 정규화는 도메인(AiReport)이 담당하고, 여기서는 생성 성공 여부만 판정한다
	private String parseTitle(String content) {
		TitleContent titleContent;
		try {
			titleContent = objectMapper.readValue(content, TitleContent.class);
		}
		catch (JacksonException e) {
			throw new ReportTitleGenerationFailedException("리포트 제목 응답이 JSON 형식이 아닙니다", e);
		}
		if (titleContent.title() == null) {
			throw new ReportTitleGenerationFailedException("리포트 제목 응답에 title 항목이 없습니다");
		}
		return titleContent.title().strip();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TitleContent(String title) {
	}
}
