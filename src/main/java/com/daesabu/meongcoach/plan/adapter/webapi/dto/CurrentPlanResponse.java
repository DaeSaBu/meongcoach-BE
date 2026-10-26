package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import java.util.Optional;

public record CurrentPlanResponse(PlanResponse plan) {

	public static CurrentPlanResponse from(Optional<CurrentPlanResult> result) {
		PlanResponse plan = result.map(PlanResponse::from).orElse(null);
		return new CurrentPlanResponse(plan);
	}
}
