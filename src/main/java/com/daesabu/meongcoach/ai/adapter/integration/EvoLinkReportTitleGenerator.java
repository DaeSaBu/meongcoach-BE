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

		return parseTitle(content);
	}

	private String completeOrThrow(String reportContentJson) {
		try {
			EvoLinkChatRequest request = buildRequest(reportContentJson);
			return chatClient.complete(request);
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

	private String parseTitle(String content) {
		TitleContent titleContent;
		try {
			titleContent = objectMapper.readValue(content, TitleContent.class);
		}
		catch (JacksonException e) {
			throw new ReportTitleGenerationFailedException("리포트 제목 응답이 JSON 형식이 아닙니다", e);
		}
		if (titleContent == null || titleContent.title() == null) {
			throw new ReportTitleGenerationFailedException("리포트 제목 응답에 title 항목이 없습니다");
		}
		String title = titleContent.title().strip();
		if (title.isBlank()) {
			throw new ReportTitleGenerationFailedException("리포트 제목 생성 결과가 비어 있습니다");
		}
		return title;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TitleContent(String title) {
	}
}
