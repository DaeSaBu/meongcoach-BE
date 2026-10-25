package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@RequiredArgsConstructor
final class AiPlanPromptBuilder {

	private final ObjectMapper objectMapper;
	private final String userPromptTemplate;

	String createUserPrompt(AiPlanGenerateRequest request) {
		ObjectNode input = objectMapper.valueToTree(request);
		ObjectNode dog = (ObjectNode) input.get("dog");

		String breedLabel = request.dog().breed().label();
		dog.put("breed", breedLabel);

		String inputJson = objectMapper.writeValueAsString(input);
		return userPromptTemplate.replace("{{input}}", inputJson);
	}
}
