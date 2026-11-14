package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.CurrentPlanResult;
import java.util.Optional;

public record CurrentPlanResponse(PlanResponse plan) {

	public static CurrentPlanResponse from(Optional<CurrentPlanResult> result) {
		PlanResponse plan = result.map(PlanResponse::from).orElse(null);
		return new CurrentPlanResponse(plan);
	}
}
