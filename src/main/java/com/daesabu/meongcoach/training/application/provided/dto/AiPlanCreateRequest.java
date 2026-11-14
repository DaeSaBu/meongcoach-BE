package com.daesabu.meongcoach.training.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiPlanCreateRequest(
		@NotBlank @Size(max = 500) String expectation,
		@Size(min = 1, max = 3) List<@NotNull Long> topicIds
) {
	public AiPlanCreateRequest(String expectation) {
		this(expectation, null);
	}
}
