package com.daesabu.meongcoach.training.domain;

public record CustomPlanCreateCommand(
		Long userId,
		Long dogId,
		String dogName
) {
}
