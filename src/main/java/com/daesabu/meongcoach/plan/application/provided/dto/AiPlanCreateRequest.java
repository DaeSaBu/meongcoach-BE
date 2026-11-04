package com.daesabu.meongcoach.plan.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiPlanCreateRequest(
		@NotNull Integer dailyMinutes,
		@NotNull Integer daysPerWeek,
		@NotBlank @Size(max = 500) String expectation
) {
}
