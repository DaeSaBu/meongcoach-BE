package com.daesabu.meongcoach.plan.domain;

public record CustomPlanCreateCommand(
		Long userId,
		Long dogId,
		String dogName,
		int dailyMinutes,
		int daysPerWeek
) {
}
