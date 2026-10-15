package com.daesabu.meongcoach.plan.application.provided.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlanCreateRequest(
		@NotNull @Positive Long dogId,
		@NotBlank String mode,
		@Size(max = 3) List<@NotNull @Positive Long> topicIds,
		@Size(max = 200) String expectation,
		@NotNull @Min(1) @Max(60) Integer dailyMinutes,
		@NotNull @Min(1) @Max(7) Integer daysPerWeek,
		boolean replace) {
}
