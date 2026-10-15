package com.daesabu.meongcoach.plan.application.provided.dto;

import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.time.LocalDate;
import java.util.List;

public record PlanDetailResult(Long id, Long dogId, String dogName, PlanMode mode, String title, String goal,
		String reason, PlanGeneratorType generator, PlanAdjustedReason adjustedReason, int durationWeeks,
		int dailyMinutes, int daysPerWeek, LocalDate startedOn, LocalDate targetEndOn, long daysLeft,
		int currentWeekNo, int expectedWeekNo, Progress progress, Today today, List<Week> weeks) {

	public record Progress(int completedItems, int totalItems) {
	}

	public record Today(Long curriculumId, String curriculumTitle, Long topicId, String topicTitle, Long lessonId,
			String lessonTitle, Integer estimatedMinutes) {
	}

	public record Week(int weekNo, String theme, String milestone, CurriculumStatus status, List<Item> items) {
	}

	public record Item(Long curriculumId, String curriculumTitle, Long topicId, String topicTitle, int totalLessons,
			int completedLessons, CurriculumStatus status) {
	}
}
