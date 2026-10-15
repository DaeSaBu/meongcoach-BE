package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Item;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Progress;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Today;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Week;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.TrainingPlanItem;
import com.daesabu.meongcoach.plan.domain.TrainingPlanWeek;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogEntry;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class PlanDetailAssembler {

	private static final int DAYS_PER_WEEK = 7;

	private PlanDetailAssembler() {
	}

	static PlanDetailResult assemble(TrainingPlan plan, String dogName, TrainingCatalogResult catalog,
			Set<Long> completedLessonIds, LocalDate today) {
		List<ResolvedWeek> resolvedWeeks = plan.getWeeks().stream()
				.map(week -> resolveWeek(plan, week, catalog, completedLessonIds))
				.toList();
		List<Week> weeks = resolvedWeeks.stream()
				.map(ResolvedWeek::toWeek)
				.toList();
		int totalItems = resolvedWeeks.stream().mapToInt(week -> week.items().size()).sum();
		int completedItems = (int) resolvedWeeks.stream()
				.flatMap(week -> week.items().stream())
				.filter(item -> item.result().status() == CurriculumStatus.COMPLETED)
				.count();
		ResolvedWeek currentWeek = currentWeek(resolvedWeeks);
		Today todayLesson = todayLesson(currentWeek, completedLessonIds);
		int currentWeekNo = currentWeek.weekNo();
		int expectedWeekNo = expectedWeekNo(plan, today);
		long daysLeft = ChronoUnit.DAYS.between(today, plan.getTargetEndOn());
		return new PlanDetailResult(plan.getId(), plan.getDogId(), dogName, plan.getMode(), plan.getTitle(),
				plan.getGoal(), plan.getReason(), plan.getGenerator(), plan.getAdjustedReason(),
				plan.getDurationWeeks(), plan.getDailyMinutes(), plan.getDaysPerWeek(), plan.getStartedOn(),
				plan.getTargetEndOn(), daysLeft, currentWeekNo, expectedWeekNo,
				new Progress(completedItems, totalItems), todayLesson, weeks);
	}

	private static ResolvedWeek resolveWeek(TrainingPlan plan, TrainingPlanWeek week, TrainingCatalogResult catalog,
			Set<Long> completedLessonIds) {
		List<ResolvedItem> items = plan.itemsOf(week.getWeekNo()).stream()
				.map(item -> resolveItem(item, catalog, completedLessonIds))
				.flatMap(Optional::stream)
				.toList();
		return new ResolvedWeek(week, items);
	}

	private static Optional<ResolvedItem> resolveItem(TrainingPlanItem item, TrainingCatalogResult catalog,
			Set<Long> completedLessonIds) {
		Optional<CatalogEntry> found = catalog.findCurriculum(item.getCurriculumId());
		if (found.isEmpty()) {
			log.debug("플랜 항목의 커리큘럼이 카탈로그에 없어 제외합니다. planId={}, curriculumId={}",
					item.getPlan().getId(), item.getCurriculumId());
			return Optional.empty();
		}
		CatalogEntry entry = found.get();
		int totalLessons = entry.curriculum().lessons().size();
		int completedLessons = (int) entry.curriculum().lessons().stream()
				.filter(lesson -> completedLessonIds.contains(lesson.id()))
				.count();
		Item result = new Item(entry.curriculum().id(), entry.curriculum().title(), entry.topic().id(),
				entry.topic().title(), totalLessons, completedLessons,
				CurriculumStatus.of(totalLessons, completedLessons), entry.curriculum().locked());
		return Optional.of(new ResolvedItem(entry, result));
	}

	private static ResolvedWeek currentWeek(List<ResolvedWeek> weeks) {
		return weeks.stream()
				.filter(week -> week.items().stream().anyMatch(item -> item.result().status() != CurriculumStatus.COMPLETED))
				.findFirst()
				.orElseGet(weeks::getLast);
	}

	private static Today todayLesson(ResolvedWeek currentWeek, Set<Long> completedLessonIds) {
		return currentWeek.items().stream()
				.filter(item -> item.result().status() != CurriculumStatus.COMPLETED)
				.map(item -> firstRemainingLesson(item, completedLessonIds))
				.flatMap(Optional::stream)
				.findFirst()
				.orElse(null);
	}

	private static Optional<Today> firstRemainingLesson(ResolvedItem item, Set<Long> completedLessonIds) {
		return item.entry().curriculum().lessons().stream()
				.sorted(Comparator.comparingInt(CatalogLesson::sortOrder))
				.filter(lesson -> !completedLessonIds.contains(lesson.id()))
				.findFirst()
				.map(lesson -> new Today(item.result().curriculumId(), item.result().curriculumTitle(),
						item.result().topicId(), item.result().topicTitle(), lesson.id(), lesson.title(),
						lesson.estimatedMinutes(), item.result().locked()));
	}

	private static int expectedWeekNo(TrainingPlan plan, LocalDate today) {
		long elapsedWeeks = ChronoUnit.DAYS.between(plan.getStartedOn(), today) / DAYS_PER_WEEK;
		long weekNo = Math.clamp(elapsedWeeks + 1, 1, plan.getDurationWeeks());
		return (int) weekNo;
	}

	private record ResolvedItem(CatalogEntry entry, Item result) {
	}

	private record ResolvedWeek(TrainingPlanWeek week, List<ResolvedItem> items) {

		int weekNo() {
			return week.getWeekNo();
		}

		Week toWeek() {
			List<Item> itemResults = items.stream()
					.map(ResolvedItem::result)
					.toList();
			return new Week(week.getWeekNo(), week.getTheme(), week.getMilestone(), status(), itemResults);
		}

		private CurriculumStatus status() {
			if (items.isEmpty()) {
				return CurriculumStatus.NOT_STARTED;
			}
			if (items.stream().allMatch(item -> item.result().status() == CurriculumStatus.COMPLETED)) {
				return CurriculumStatus.COMPLETED;
			}
			if (items.stream().anyMatch(item -> item.result().status() != CurriculumStatus.NOT_STARTED)) {
				return CurriculumStatus.IN_PROGRESS;
			}
			return CurriculumStatus.NOT_STARTED;
		}
	}
}
