package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.exception.InvalidAiPlanResponseException;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class AiPlanGenerationAdapter {

	private final AiPlanPromptBuilder promptBuilder;
	private final AiContentGenerator contentGenerator;
	private final String systemPrompt;
	private final String responseSchema;

	String generateContent(AiPlanGenerateRequest request) {
		String userPrompt = promptBuilder.createUserPrompt(request);
		AiContentGenerateRequest contentRequest = new AiContentGenerateRequest(
				systemPrompt,
				userPrompt,
				responseSchema
		);

		return contentGenerator.generate(contentRequest);
	}

	void validateResult(AiPlanGenerateResult result, AiPlanGenerateRequest request) {
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
