package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.domain.KoreanParticle;
import com.daesabu.meongcoach.plan.domain.LifeStage;
import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.exception.PlanCatalogEmptyException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicNotFoundException;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class FallbackPlanBuilder {

	private static final int FALLBACK_TOPIC_LIMIT = 2;
	private static final int WEEKS = 2;

	public FallbackResult buildManual(PlanGenerationContext context, List<Long> topicIds) {
		List<CatalogTopic> allTopics = allTopics(context.catalog().categories());
		topicIds.stream()
				.filter(topicId -> allTopics.stream().noneMatch(topic -> topic.id().equals(topicId)))
				.findFirst()
				.ifPresent(topicId -> {
					throw new PlanTopicNotFoundException(topicId);
				});
		List<CatalogTopic> selected = allTopics.stream()
				.filter(topic -> topicIds.contains(topic.id()))
				.filter(FallbackPlanBuilder::hasOpenCurriculum)
				.toList();
		return arrange(context, selected, PlanGeneratorType.MANUAL);
	}

	public FallbackResult buildFallback(PlanGenerationContext context) {
		LifeStage stage = LifeStage.of(context.dog().ageInMonths());
		List<CatalogTopic> candidates = allTopics(prioritize(context.catalog().categories(), stage)).stream()
				.filter(FallbackPlanBuilder::hasOpenCurriculum)
				.toList();
		List<CatalogTopic> unfinished = candidates.stream()
				.filter(topic -> hasUnfinishedOpenCurriculum(topic, context.completedLessonIds()))
				.limit(FALLBACK_TOPIC_LIMIT)
				.toList();
		if (!unfinished.isEmpty()) {
			return arrange(context, unfinished, PlanGeneratorType.FALLBACK);
		}
		List<CatalogTopic> review = candidates.stream()
				.limit(FALLBACK_TOPIC_LIMIT)
				.toList();
		return arrange(context, review, PlanGeneratorType.FALLBACK);
	}

	private FallbackResult arrange(PlanGenerationContext context, List<CatalogTopic> topics,
			PlanGeneratorType generatorType) {
		if (topics.isEmpty()) {
			throw new PlanCatalogEmptyException();
		}
		Set<Long> completedLessonIds = context.completedLessonIds();
		List<Placement> placements = arrangeOrder(topics, completedLessonIds);
		boolean allCompleted = placements.stream().allMatch(placement -> isCompleted(placement.curriculum(), completedLessonIds));
		int durationWeeks = durationWeeks(placements.size());
		PlanAdjustedReason adjustedReason = adjustedReason(placements.size(), allCompleted);
		List<WeekDraft> weeks = splitWeeks(placements, durationWeeks);
		PlanDraft draft = new PlanDraft(title(context, topics, durationWeeks), goal(topics, durationWeeks),
				reason(context, topics), weeks);
		return new FallbackResult(draft, durationWeeks, adjustedReason, generatorType);
	}

	private List<Placement> arrangeOrder(List<CatalogTopic> topics, Set<Long> completedLessonIds) {
		List<Placement> ordered = topics.stream()
				.flatMap(topic -> topic.curriculums().stream()
						.filter(curriculum -> !curriculum.locked())
						.sorted(Comparator.comparingInt(CatalogCurriculum::sortOrder))
						.map(curriculum -> new Placement(topic, curriculum)))
				.toList();
		List<Placement> result = new ArrayList<>();
		ordered.stream()
				.filter(placement -> !isCompleted(placement.curriculum(), completedLessonIds))
				.forEach(result::add);
		ordered.stream()
				.filter(placement -> isCompleted(placement.curriculum(), completedLessonIds))
				.forEach(result::add);
		return result;
	}

	private int durationWeeks(int curriculumCount) {
		if (curriculumCount == 1) {
			return 1;
		}
		return WEEKS;
	}

	private PlanAdjustedReason adjustedReason(int curriculumCount, boolean allCompleted) {
		if (allCompleted) {
			return PlanAdjustedReason.REVIEW;
		}
		if (curriculumCount == 1) {
			return PlanAdjustedReason.DURATION_SHORTENED;
		}
		return null;
	}

	private List<WeekDraft> splitWeeks(List<Placement> placements, int durationWeeks) {
		int firstWeekSize = (placements.size() + durationWeeks - 1) / durationWeeks;
		List<WeekDraft> weeks = new ArrayList<>();
		weeks.add(weekDraft(1, placements.subList(0, firstWeekSize)));
		if (durationWeeks > 1) {
			weeks.add(weekDraft(2, placements.subList(firstWeekSize, placements.size())));
		}
		return weeks;
	}

	private WeekDraft weekDraft(int weekNo, List<Placement> placements) {
		String theme = placements.stream()
				.map(placement -> placement.topic().title())
				.distinct()
				.reduce((left, right) -> left + "·" + right)
				.orElseThrow();
		String milestone = placements.getLast().curriculum().title() + "까지 해내기";
		List<Long> curriculumIds = placements.stream()
				.map(placement -> placement.curriculum().id())
				.toList();
		return new WeekDraft(weekNo, theme, milestone, curriculumIds);
	}

	private String title(PlanGenerationContext context, List<CatalogTopic> topics, int durationWeeks) {
		return context.dog().name() + "의 " + durationWeeks + "주 " + topics.getFirst().title() + " 플랜";
	}

	private String goal(List<CatalogTopic> topics, int durationWeeks) {
		String joined = joinTitles(topics);
		return durationWeeks + "주 동안 " + joined + KoreanParticle.objectOf(joined) + " 차근차근 익혀요";
	}

	private String reason(PlanGenerationContext context, List<CatalogTopic> topics) {
		String joined = joinTitles(topics);
		String subject = subject(context);
		return "이렇게 짰어요: " + joined + KoreanParticle.objectOf(joined) + " " + subject
				+ "에게 맞게 기초부터 단계별로 이어지도록 묶고, 하루 " + context.dailyMinutes() + "분 · 주 "
				+ context.daysPerWeek() + "일에 맞췄어요.";
	}

	private String subject(PlanGenerationContext context) {
		String age = LifeStage.formatAge(context.dog().ageInMonths(), context.dog().birthDateEstimated());
		if (age.isEmpty()) {
			return context.dog().name();
		}
		return age + " " + context.dog().name();
	}

	private String joinTitles(List<CatalogTopic> topics) {
		return topics.stream()
				.map(CatalogTopic::title)
				.reduce((left, right) -> left + ", " + right)
				.orElseThrow();
	}

	private List<CatalogCategory> prioritize(List<CatalogCategory> categories, LifeStage stage) {
		List<EntitlementType> order = stage.priority().stream()
				.map(LifeStage::toEntitlementType)
				.toList();
		return categories.stream()
				.sorted(Comparator.comparingInt(category -> rank(order, category)))
				.toList();
	}

	private int rank(List<EntitlementType> order, CatalogCategory category) {
		int index = order.indexOf(category.requiredEntitlementType());
		if (index < 0) {
			return order.size();
		}
		return index;
	}

	private List<CatalogTopic> allTopics(List<CatalogCategory> categories) {
		return categories.stream()
				.flatMap(category -> category.topics().stream())
				.toList();
	}

	private static boolean hasOpenCurriculum(CatalogTopic topic) {
		return openCurriculums(topic).findAny().isPresent();
	}

	private static boolean hasUnfinishedOpenCurriculum(CatalogTopic topic, Set<Long> completedLessonIds) {
		return openCurriculums(topic).anyMatch(curriculum -> !isCompleted(curriculum, completedLessonIds));
	}

	private static Stream<CatalogCurriculum> openCurriculums(CatalogTopic topic) {
		return topic.curriculums().stream()
				.filter(curriculum -> !curriculum.locked());
	}

	private static boolean isCompleted(CatalogCurriculum curriculum, Set<Long> completedLessonIds) {
		return !curriculum.lessons().isEmpty()
				&& curriculum.lessons().stream().allMatch(lesson -> completedLessonIds.contains(lesson.id()));
	}

	public record FallbackResult(PlanDraft draft, int durationWeeks, PlanAdjustedReason adjustedReason,
			PlanGeneratorType generatorType) {
	}

	private record Placement(CatalogTopic topic, CatalogCurriculum curriculum) {
	}
}
