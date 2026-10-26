package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ChatMessage;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ResponseFormat;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.Thinking;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.ai.domain.exception.AiContentGenerationFailedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EvoLinkContentGenerator implements AiContentGenerator {

	private final EvoLinkChatClient chatClient;
	private final EvoLinkProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String generate(AiContentGenerateRequest request) {
		EvoLinkChatRequest chatRequest = buildRequest(request);
		try {
			return chatClient.complete(chatRequest);
		} catch (RestClientException | EvoLinkResponseException exception) {
			throw new AiContentGenerationFailedException(exception);
		}
	}

	private EvoLinkChatRequest buildRequest(AiContentGenerateRequest request) {
		JsonNode responseSchema = objectMapper.readTree(request.responseSchema());
		if (responseSchema == null || !responseSchema.isObject()) {
			throw new IllegalArgumentException("응답 스키마는 JSON 객체여야 합니다.");
		}
		List<ChatMessage> messages = List.of(
				ChatMessage.system(request.systemPrompt()),
				ChatMessage.user(request.userPrompt()));
		ResponseFormat responseFormat = ResponseFormat.jsonSchema("generated_content", responseSchema);
		String model = properties.model();
		Integer maxTokens = properties.maxTokens();
		Double temperature = properties.temperature();
		Thinking thinking = new Thinking(properties.thinking());
		return new EvoLinkChatRequest(model, messages, responseFormat, maxTokens, temperature, thinking);
	}
}
