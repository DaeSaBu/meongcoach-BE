package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.exception.InvalidAiPlanResponseException;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
final class AiPlanGenerationAdapter implements AiPlanGenerator {

	private final AiPlanPromptBuilder promptBuilder;
	private final AiContentGenerator contentGenerator;
	private final String systemPrompt;
	private final String responseSchema;
	private final ObjectMapper objectMapper;

	@Override
	public AiPlanGenerateResult generate(AiPlanGenerateRequest request) {
		String content = generateContent(request);
		AiPlanGenerateResult result = parseResult(content);
		validateResult(result, request);
		return result;
	}

	private AiPlanGenerateResult parseResult(String content) {
		if (content == null || content.isBlank()) {
			throw new InvalidAiPlanResponseException();
		}
		try {
			JsonNode response = objectMapper.reader()
					.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
					.with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
					.readTree(content);
			if (!response.isObject() || response.size() != 2
					|| !response.path("topicIds").isArray()
					|| !response.path("goal").isString()) {
				throw new InvalidAiPlanResponseException();
			}
			for (JsonNode topicId : response.get("topicIds")) {
				if (!topicId.isIntegralNumber() || !topicId.canConvertToLong()) {
					throw new InvalidAiPlanResponseException();
				}
			}
			return objectMapper.treeToValue(response, AiPlanGenerateResult.class);
		} catch (JacksonException exception) {
			throw new InvalidAiPlanResponseException(exception);
		}
	}

	private String generateContent(AiPlanGenerateRequest request) {
		String userPrompt = promptBuilder.createUserPrompt(request);
		AiContentGenerateRequest contentRequest = new AiContentGenerateRequest(
				systemPrompt,
				userPrompt,
				responseSchema
		);

		return contentGenerator.generate(contentRequest);
	}

	private void validateResult(AiPlanGenerateResult result, AiPlanGenerateRequest request) {
		if (result == null
				|| result.topicIds() == null
				|| result.topicIds().isEmpty()
				|| result.goal() == null
				|| result.goal().isBlank()) {
			throw new InvalidAiPlanResponseException();
		}

		Set<Long> candidateTopicIds = request.curriculums().stream()
				.map(AiPlanCurriculumResult::topicId)
				.collect(Collectors.toSet());

		Set<Long> selectedTopicIds = new HashSet<>();

		for (Long topicId : result.topicIds()) {
			if (topicId == null || !candidateTopicIds.contains(topicId)) {
				throw new InvalidAiPlanResponseException();
			}
			if (!selectedTopicIds.add(topicId)) {
				throw new InvalidAiPlanResponseException();
			}
		}
	}
}
