package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;

public record CurrentPlanResponse(boolean hasCompletedPlan, PlanDetailResponse plan) {

	public static CurrentPlanResponse from(CurrentPlanResult result) {
		PlanDetailResult plan = result.plan();
		if (plan == null) {
			return new CurrentPlanResponse(result.hasCompletedPlan(), null);
		}
		PlanDetailResponse detail = PlanDetailResponse.from(plan);
		return new CurrentPlanResponse(result.hasCompletedPlan(), detail);
	}
}
