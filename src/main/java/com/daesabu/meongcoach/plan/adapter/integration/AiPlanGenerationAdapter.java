package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.llm.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.llm.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.llm.application.provided.PromptTemplateLoader;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.exception.InvalidAiPlanResponseException;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@Component
final class AiPlanGenerationAdapter implements AiPlanGenerator {

	private static final String SYSTEM_PROMPT_PATH = "prompts/plan-generation/system.md";
	private static final String USER_PROMPT_PATH = "prompts/plan-generation/user.md";
	private static final String SCHEMA_PATH = "prompts/plan-generation/schema.json";
	private static final String TOPIC_IDS_FIELD = "topicIds";
	private static final String GOAL_FIELD = "goal";
	private static final List<String> REASONING_FIELDS = List.of("interpretation", "decision");
	private static final Set<String> RESPONSE_FIELDS = Set.of("interpretation", "decision", TOPIC_IDS_FIELD, GOAL_FIELD);

	private final AiPlanPromptBuilder promptBuilder;
	private final AiContentGenerator contentGenerator;
	private final String systemPrompt;
	private final String responseSchema;
	private final ObjectMapper objectMapper;

	AiPlanGenerationAdapter(AiContentGenerator contentGenerator, ObjectMapper objectMapper,
			PromptTemplateLoader promptTemplateLoader) {
		this.contentGenerator = contentGenerator;
		this.objectMapper = objectMapper;
		this.systemPrompt = promptTemplateLoader.load(SYSTEM_PROMPT_PATH);
		this.responseSchema = promptTemplateLoader.load(SCHEMA_PATH);
		String userPromptTemplate = promptTemplateLoader.load(USER_PROMPT_PATH);
		this.promptBuilder = new AiPlanPromptBuilder(objectMapper, userPromptTemplate);
	}

	@Override
	public AiPlanGenerateResult generate(AiPlanGenerateRequest request) {
		String content = generateContent(request);
		JsonNode response = readResponse(content);
		logReasoning(response, request);
		AiPlanGenerateResult result = parseResult(response);
		validateResult(result, request);
		return result;
	}

	private JsonNode readResponse(String content) {
		if (content == null || content.isBlank()) {
			throw new InvalidAiPlanResponseException();
		}
		try {
			JsonNode response = objectMapper.reader()
					.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
					.with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
					.readTree(content);
			validateShape(response);
			return response;
		} catch (JacksonException exception) {
			throw new InvalidAiPlanResponseException(exception);
		}
	}

	private void validateShape(JsonNode response) {
		if (!response.isObject()
				|| !RESPONSE_FIELDS.containsAll(response.propertyNames())
				|| !response.path(TOPIC_IDS_FIELD).isArray()
				|| !response.path(GOAL_FIELD).isString()) {
			throw new InvalidAiPlanResponseException();
		}
		for (JsonNode topicId : response.get(TOPIC_IDS_FIELD)) {
			if (!topicId.isIntegralNumber() || !topicId.canConvertToLong()) {
				throw new InvalidAiPlanResponseException();
			}
		}
	}

	private void logReasoning(JsonNode response, AiPlanGenerateRequest request) {
		ObjectNode reasoning = objectMapper.createObjectNode();
		reasoning.put("expectation", request.expectation());
		REASONING_FIELDS.stream()
				.filter(response::has)
				.forEach(field -> reasoning.set(field, response.get(field)));
		reasoning.set(TOPIC_IDS_FIELD, response.get(TOPIC_IDS_FIELD));
		log.info("플랜 생성 근거: {} {}", request.dog().id(), reasoning);
	}

	private AiPlanGenerateResult parseResult(JsonNode response) {
		ObjectNode generated = objectMapper.createObjectNode();
		generated.set(TOPIC_IDS_FIELD, response.get(TOPIC_IDS_FIELD));
		generated.set(GOAL_FIELD, response.get(GOAL_FIELD));
		try {
			return objectMapper.treeToValue(generated, AiPlanGenerateResult.class);
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

		if (!result.topicIds().containsAll(request.selectedTopicIds())) {
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
