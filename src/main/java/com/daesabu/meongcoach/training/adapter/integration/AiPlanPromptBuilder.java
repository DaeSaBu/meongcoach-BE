package com.daesabu.meongcoach.training.adapter.integration;

import com.daesabu.meongcoach.training.application.required.dto.AiPlanGenerateRequest;
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
		dog.remove("sex");
		dog.remove("id");

		String inputJson = objectMapper.writeValueAsString(input);
		return userPromptTemplate.replace("{{input}}", inputJson);
	}
}
