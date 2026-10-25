package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
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
}
