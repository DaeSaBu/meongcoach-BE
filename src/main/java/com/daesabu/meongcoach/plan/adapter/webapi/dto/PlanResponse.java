package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record PlanResponse(Long planId, Long dogId, String title, String goal, int dailyMinutes, int daysPerWeek,
		PlanStatus status, LocalDateTime createdAt, Instant completedAt, List<PlanCurriculumResponse> curriculums) {

	public static PlanResponse from(CurrentPlanResult result) {
		Plan plan = result.plan();
		List<PlanCurriculumResponse> curriculums = result.curriculums().stream().map(PlanCurriculumResponse::from).toList();
		return new PlanResponse(plan.getId(), plan.getDogId(), plan.getTitle(), plan.getGoal(), plan.getDailyMinutes(),
				plan.getDaysPerWeek(), plan.getStatus(), plan.getCreatedAt(), plan.getCompletedAt(), curriculums);
	}
}
