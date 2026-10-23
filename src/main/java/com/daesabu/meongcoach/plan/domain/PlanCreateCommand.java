package com.daesabu.meongcoach.plan.domain;

public record PlanCreateCommand(
		Long userId,
		Long dogId,
		String title,
		String goal,
		int dailyMinutes,
		int daysPerWeek
) {
}
