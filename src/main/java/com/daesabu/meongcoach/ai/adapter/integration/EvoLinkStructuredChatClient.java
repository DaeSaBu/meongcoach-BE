package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import com.daesabu.meongcoach.ai.domain.shared.StructuredChatCompletionFailedException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EvoLinkStructuredChatClient implements StructuredChatClient {

	private final EvoLinkChatClient chatClient;
	private final EvoLinkProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String complete(StructuredChatRequest request) {
		try {
			return chatClient.complete(buildRequest(request), request.timeout());
		}
		catch (RestClientException | EvoLinkResponseException | JacksonException e) {
			throw new StructuredChatCompletionFailedException("구조화 JSON 채팅 모델 호출에 실패했습니다", e);
		}
	}

	private EvoLinkChatRequest buildRequest(StructuredChatRequest request) {
		Map<?, ?> schema = objectMapper.readValue(request.schemaJson(), Map.class);
		return new EvoLinkChatRequest(
				properties.model(),
				List.of(
						ChatMessage.system(request.systemPrompt()),
						ChatMessage.user(request.userPrompt())),
				ResponseFormat.jsonSchema(request.schemaName(), schema),
				properties.maxTokens(),
				properties.temperature(),
				new Thinking(properties.thinking()));
	}
}
