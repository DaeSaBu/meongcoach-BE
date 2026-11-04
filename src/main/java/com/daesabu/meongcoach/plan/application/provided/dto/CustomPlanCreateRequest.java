package com.daesabu.meongcoach.plan.application.provided.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CustomPlanCreateRequest(
		@NotNull @Size(min = 1, max = 3) List<@NotNull Long> topicIds,
		@NotNull Integer dailyMinutes,
		@NotNull Integer daysPerWeek
) {
}
