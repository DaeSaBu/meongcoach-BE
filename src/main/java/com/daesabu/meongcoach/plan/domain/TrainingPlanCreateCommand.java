package com.daesabu.meongcoach.plan.domain;

import java.time.LocalDate;

public record TrainingPlanCreateCommand(Long userId, Long dogId, PlanMode mode, PlanGeneratorType generator,
		PlanAdjustedReason adjustedReason, int durationWeeks, int dailyMinutes, int daysPerWeek, LocalDate startedOn,
		String interviewJson, PlanDraft draft) {
}
