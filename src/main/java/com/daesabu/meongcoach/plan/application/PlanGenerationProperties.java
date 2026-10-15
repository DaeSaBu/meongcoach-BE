package com.daesabu.meongcoach.plan.application;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.plan.generation")
public record PlanGenerationProperties(
		@NotNull @DefaultValue("PT30S") Duration callTimeout,
		@NotNull @DefaultValue("PT75S") Duration totalBudget
) {
}
