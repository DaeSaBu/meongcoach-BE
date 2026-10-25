package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import java.time.Instant;
import java.util.List;

public record PlanOutcomeResponse(Long planId, PlanOutcome outcome, Instant completedAt, int completedCurriculumCount,
		int elapsedDays, List<PlanTopicResponse> topics) {

	public static PlanOutcomeResponse from(PlanOutcomeResult result) {
		Plan plan = result.plan();
		List<PlanTopicResponse> topics = result.topics().stream().map(PlanTopicResponse::from).toList();
		return new PlanOutcomeResponse(plan.getId(), plan.getOutcome(), plan.getCompletedAt(),
				plan.getCurriculumIds().size(), result.elapsedDays(), topics);
	}
}
