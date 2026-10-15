package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.time.LocalDate;
import java.util.List;

public record PlanDetailResponse(Long planId, Long dogId, String dogName, PlanMode mode, String title, String goal,
		String reason, PlanGeneratorType generator, PlanAdjustedReason adjustedReason, int durationWeeks,
		int dailyMinutes, int daysPerWeek, LocalDate startedOn, LocalDate targetEndOn, long daysLeft,
		int currentWeekNo, int expectedWeekNo, ProgressResponse progress, TodayResponse today,
		List<WeekResponse> weeks) {

	public static PlanDetailResponse from(PlanDetailResult result) {
		ProgressResponse progress = ProgressResponse.from(result.progress());
		TodayResponse today = TodayResponse.from(result.today());
		List<WeekResponse> weeks = result.weeks().stream()
				.map(WeekResponse::from)
				.toList();
		return new PlanDetailResponse(result.id(), result.dogId(), result.dogName(), result.mode(), result.title(),
				result.goal(), result.reason(), result.generator(), result.adjustedReason(), result.durationWeeks(),
				result.dailyMinutes(), result.daysPerWeek(), result.startedOn(), result.targetEndOn(),
				result.daysLeft(), result.currentWeekNo(), result.expectedWeekNo(), progress, today, weeks);
	}

	public record ProgressResponse(int completedItems, int totalItems) {

		static ProgressResponse from(PlanDetailResult.Progress progress) {
			return new ProgressResponse(progress.completedItems(), progress.totalItems());
		}
	}

	public record TodayResponse(Long curriculumId, String curriculumTitle, Long topicId, String topicTitle,
			Long lessonId, String lessonTitle, Integer estimatedMinutes, boolean locked) {

		static TodayResponse from(PlanDetailResult.Today today) {
			if (today == null) {
				return null;
			}
			return new TodayResponse(today.curriculumId(), today.curriculumTitle(), today.topicId(),
					today.topicTitle(), today.lessonId(), today.lessonTitle(), today.estimatedMinutes(), today.locked());
		}
	}

	public record WeekResponse(int weekNo, String theme, String milestone, CurriculumStatus status,
			List<ItemResponse> items) {

		static WeekResponse from(PlanDetailResult.Week week) {
			List<ItemResponse> items = week.items().stream()
					.map(ItemResponse::from)
					.toList();
			return new WeekResponse(week.weekNo(), week.theme(), week.milestone(), week.status(), items);
		}
	}

	public record ItemResponse(Long curriculumId, String curriculumTitle, Long topicId, String topicTitle,
			int totalLessons, int completedLessons, CurriculumStatus status, boolean locked) {

		static ItemResponse from(PlanDetailResult.Item item) {
			return new ItemResponse(item.curriculumId(), item.curriculumTitle(), item.topicId(), item.topicTitle(),
					item.totalLessons(), item.completedLessons(), item.status(), item.locked());
		}
	}
}
