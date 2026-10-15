package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.ai.application.provided.StructuredChatCompleter;
import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.domain.shared.StructuredChatCompletionFailedException;
import com.daesabu.meongcoach.plan.application.PlanGenerationContext;
import com.daesabu.meongcoach.plan.application.required.PlanGenerator;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.exception.PlanGenerationFailedException;
import com.daesabu.meongcoach.shared.prompt.PromptLoader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EvoLinkPlanGenerator implements PlanGenerator {

	private static final String SYSTEM_PROMPT_PATH = "prompts/plan-generation/system.md";
	private static final String USER_PROMPT_PATH = "prompts/plan-generation/user.md";
	private static final String SCHEMA_PATH = "prompts/plan-generation/schema.json";
	private static final String SCHEMA_NAME = "training_plan";
	private static final List<String> PLACEHOLDERS = List.of("dog", "owner", "history", "catalog", "retryFeedback");
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

	private final StructuredChatCompleter structuredChatCompleter;
	private final ObjectMapper objectMapper;
	private final String systemPrompt;
	private final String userPrompt;
	private final String schemaJson;

	public EvoLinkPlanGenerator(StructuredChatCompleter structuredChatCompleter, ObjectMapper objectMapper) {
		this.structuredChatCompleter = structuredChatCompleter;
		this.objectMapper = objectMapper;
		this.systemPrompt = PromptLoader.load(SYSTEM_PROMPT_PATH);
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		this.schemaJson = PromptLoader.load(SCHEMA_PATH);
		validatePlaceholders();
	}

	@Override
	public PlanDraft generate(PlanGenerationContext context, String retryFeedback) {
		StructuredChatRequest request = new StructuredChatRequest(systemPrompt, userPrompt(context, retryFeedback),
				SCHEMA_NAME, schemaJson);
		String content = completeOrThrow(request);
		return parseDraft(content);
	}

	private void validatePlaceholders() {
		PLACEHOLDERS.stream()
				.filter(name -> !userPrompt.contains("{{" + name + "}}"))
				.findFirst()
				.ifPresent(name -> {
					throw new IllegalStateException("사용자 프롬프트에 {{" + name + "}} 자리가 없습니다: " + USER_PROMPT_PATH);
				});
	}

	private String userPrompt(PlanGenerationContext context, String retryFeedback) {
		Map<String, String> values = Map.of(
				"dog", PlanPromptSections.dog(context.dog()),
				"owner", PlanPromptSections.owner(context),
				"history", PlanPromptSections.history(context),
				"catalog", PlanPromptSections.catalog(context.catalog()),
				"retryFeedback", PlanPromptSections.retryFeedback(retryFeedback));
		return PLACEHOLDER_PATTERN.matcher(userPrompt)
				.replaceAll(match -> Matcher.quoteReplacement(values.getOrDefault(match.group(1), match.group())))
				.strip();
	}

	private String completeOrThrow(StructuredChatRequest request) {
		try {
			return structuredChatCompleter.complete(request);
		} catch (StructuredChatCompletionFailedException e) {
			throw new PlanGenerationFailedException("플랜 생성 모델 호출에 실패했습니다", e);
		}
	}

	private PlanDraft parseDraft(String content) {
		if (content == null || content.isBlank()) {
			throw new PlanGenerationFailedException("플랜 생성 응답이 비어 있습니다");
		}
		DraftContent draft;
		try {
			draft = objectMapper.readValue(content, DraftContent.class);
		} catch (JacksonException e) {
			throw new PlanGenerationFailedException("플랜 생성 응답이 JSON 형식이 아닙니다", e);
		}
		if (draft == null) {
			throw new PlanGenerationFailedException("플랜 생성 응답이 비어 있습니다");
		}
		return draft.toDraft();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DraftContent(String title, String goal, String reason, List<WeekContent> weeks) {

		PlanDraft toDraft() {
			List<WeekContent> contents = Objects.requireNonNullElse(weeks, List.of());
			List<WeekDraft> weekDrafts = contents.stream()
					.map(WeekContent::toDraft)
					.toList();
			return new PlanDraft(title, goal, reason, weekDrafts);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record WeekContent(int weekNo, String theme, String milestone, List<Long> curriculumIds) {

		WeekDraft toDraft() {
			List<Long> ids = Objects.requireNonNullElse(curriculumIds, List.of());
			return new WeekDraft(weekNo, theme, milestone, ids);
		}
	}
}
