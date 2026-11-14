package com.daesabu.meongcoach.training.domain;

public record AiPlanCreateCommand(
		Long userId,
		Long dogId,
		String dogName,
		String goal,
		String expectation
) {
}
