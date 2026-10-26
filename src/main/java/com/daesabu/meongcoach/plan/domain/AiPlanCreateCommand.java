package com.daesabu.meongcoach.plan.domain;

public record AiPlanCreateCommand(
		Long userId,
		Long dogId,
		String dogName,
		String goal,
		int dailyMinutes,
		int daysPerWeek,
		String expectation
) {
}
