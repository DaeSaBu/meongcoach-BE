package com.daesabu.meongcoach.plan.adapter.integration;

import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.plan.application.PlanCatalogRules;
import com.daesabu.meongcoach.plan.application.PlanGenerationContext;
import com.daesabu.meongcoach.plan.domain.LifeStage;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.jackson.databind.json.JsonMapper;

final class PlanPromptSections {

	private static final String NONE = "없음";
	private static final String SEPARATOR = " / ";
	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

	private PlanPromptSections() {
	}

	static String dog(DogProfileResult dog) {
		List<String> parts = Stream.of(
				"이름: " + dog.name(),
				"월령: " + age(dog),
				optional("견종", dog.breedLabel()),
				optional("성별", sex(dog.sex())),
				optional("성격", personality(dog.personalityLabels())),
				weight(dog.weightKg()))
				.filter(part -> !part.isEmpty())
				.toList();
		return String.join(SEPARATOR, parts);
	}

	static String owner(PlanGenerationContext context) {
		return String.join(SEPARATOR,
				"기대사항: \"" + context.expectation() + "\"",
				"하루 연습: " + context.dailyMinutes() + "분",
				"주 연습일: " + context.daysPerWeek() + "일");
	}

	static String history(PlanGenerationContext context) {
		String topics = joinOrNone(context.priorTopicTitles(), ", ");
		List<Long> completedCurriculumIds = completedCurriculumIds(context.catalog(), context.completedLessonIds());
		String curriculums = orNone(completedCurriculumIds.isEmpty(), completedCurriculumIds.toString());
		return "배운 토픽: " + topics + SEPARATOR + "완료한 커리큘럼 ID: " + curriculums;
	}

	static String catalog(TrainingCatalogResult catalog) {
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream()
						.flatMap(topic -> topic.curriculums().stream()
								.filter(PlanCatalogRules::isPlannable)
								.map(curriculum -> catalogLine(category, topic, curriculum))))
				.map(PlanPromptSections::toJson)
				.collect(Collectors.joining("\n"));
	}

	static String retryFeedback(String feedback) {
		if (feedback == null || feedback.isBlank()) {
			return "";
		}
		return "이전 응답에서 다음 문제가 있었습니다. 고쳐서 다시 만드세요: " + feedback;
	}

	private static String age(DogProfileResult dog) {
		Integer ageInMonths = dog.ageInMonths();
		if (ageInMonths == null) {
			return "알 수 없음";
		}
		String age = LifeStage.formatAge(ageInMonths, false);
		if (!dog.birthDateEstimated()) {
			return age;
		}
		return "약 " + age + "(추정 월령, 구간 \"" + LifeStage.ageRange(ageInMonths) + "\")";
	}

	private static String optional(String label, String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return label + ": " + value;
	}

	private static String personality(List<String> labels) {
		if (labels == null) {
			return "";
		}
		return String.join(", ", labels);
	}

	private static String sex(String sex) {
		if ("MALE".equals(sex)) {
			return "수컷";
		}
		if ("FEMALE".equals(sex)) {
			return "암컷";
		}
		return "";
	}

	private static String weight(BigDecimal weightKg) {
		if (weightKg == null) {
			return "";
		}
		return "몸무게: " + weightKg.stripTrailingZeros().toPlainString() + "kg";
	}

	private static String joinOrNone(List<String> values, String delimiter) {
		String joined = String.join(delimiter, values);
		return orNone(values.isEmpty(), joined);
	}

	private static String orNone(boolean empty, String text) {
		if (empty) {
			return NONE;
		}
		return text;
	}

	private static List<Long> completedCurriculumIds(TrainingCatalogResult catalog, Set<Long> completedLessonIds) {
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream())
				.flatMap(topic -> topic.curriculums().stream())
				.filter(curriculum -> curriculum.isCompleted(completedLessonIds))
				.map(CatalogCurriculum::id)
				.toList();
	}

	private static CatalogLine catalogLine(CatalogCategory category, CatalogTopic topic,
			CatalogCurriculum curriculum) {
		int minutes = curriculum.lessons().stream()
				.map(CatalogLesson::estimatedMinutes)
				.filter(value -> value != null)
				.mapToInt(Integer::intValue)
				.sum();
		return new CatalogLine(curriculum.id(), category.title(), topic.title(), curriculum.title(),
				curriculum.description(), curriculum.lessons().size(), minutes);
	}

	private static String toJson(CatalogLine line) {
		return JSON_MAPPER.writeValueAsString(line);
	}

	record CatalogLine(Long curriculumId, String stage, String topic, String title, String description, int lessons,
			int minutes) {
	}
}
