package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.ai.domain.exception.AiContentGenerationFailedException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class GeminiContentGenerator implements AiContentGenerator {

	private static final String JSON_MIME_TYPE = "application/json";
	private static final int CANDIDATE_COUNT = 1;
	private static final TypeReference<Map<String, Object>> SCHEMA_TYPE = new TypeReference<>() {
	};

	private final Client client;
	private final GeminiProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String generate(AiContentGenerateRequest request) {
		GenerateContentConfig config = buildConfig(request);
		GenerateContentResponse response;
		try {
			response = client.models.generateContent(properties.model(), request.userPrompt(), config);
		} catch (ApiException | GenAiIOException e) {
			throw new AiContentGenerationFailedException("콘텐츠 생성 모델 호출에 실패했습니다", e);
		}
		try {
			return GeminiResponses.extractText(response);
		} catch (GeminiResponseException e) {
			throw new AiContentGenerationFailedException(e.getMessage(), e);
		}
	}

	private GenerateContentConfig buildConfig(AiContentGenerateRequest request) {
		Map<String, Object> responseSchema = parseSchema(request.responseSchema());
		return GenerateContentConfig.builder()
				.systemInstruction(Content.fromParts(Part.fromText(request.systemPrompt())))
				.responseMimeType(JSON_MIME_TYPE)
				.responseJsonSchema(responseSchema)
				.candidateCount(CANDIDATE_COUNT)
				.maxOutputTokens(properties.maxOutputTokens())
				.temperature(properties.temperature())
				.thinkingConfig(ThinkingConfig.builder().thinkingLevel(properties.thinkingLevel()))
				.build();
	}

	private Map<String, Object> parseSchema(String responseSchema) {
		JsonNode schema = objectMapper.readTree(responseSchema);
		if (schema == null || !schema.isObject()) {
			throw new IllegalArgumentException("응답 스키마는 JSON 객체여야 합니다.");
		}
		return objectMapper.convertValue(schema, SCHEMA_TYPE);
	}
}
