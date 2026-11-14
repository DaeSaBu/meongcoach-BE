package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Plan;

public record PlanCreateResponse(Long planId) {

	public static PlanCreateResponse from(Plan plan) {
		return new PlanCreateResponse(plan.getId());
	}
}
