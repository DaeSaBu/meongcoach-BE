package com.daesabu.meongcoach.training.domain;

public record PlanCreateCommand(
		Long userId,
		Long dogId,
		String title,
		String goal
) {
}
