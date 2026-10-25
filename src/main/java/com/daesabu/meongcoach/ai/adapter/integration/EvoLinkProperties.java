package com.daesabu.meongcoach.ai.adapter.integration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.ai.evolink")
public record EvoLinkProperties(
		@NotBlank String model,
		@NotNull @Positive Integer maxTokens,
		@NotNull @PositiveOrZero Double temperature,
		@NotBlank String thinking,
		@NotNull @DecimalMin("0.2") @DecimalMax("5.0") Double videoFps
) {
}
