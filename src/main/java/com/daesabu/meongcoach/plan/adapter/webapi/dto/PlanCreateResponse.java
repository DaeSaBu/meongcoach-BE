package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.domain.Plan;

public record PlanCreateResponse(Long planId) {

	public static PlanCreateResponse from(Plan plan) {
		return new PlanCreateResponse(plan.getId());
	}
}
