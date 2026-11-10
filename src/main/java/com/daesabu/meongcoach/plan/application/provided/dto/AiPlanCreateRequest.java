package com.daesabu.meongcoach.plan.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPlanCreateRequest(
		@NotBlank @Size(max = 500) String expectation
) {
}
