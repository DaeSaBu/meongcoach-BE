package com.daesabu.meongcoach.ai.adapter.integration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.ai.gemini")
public record GeminiProperties(
		@NotBlank String apiKey,
		@NotBlank String model,
		@NotNull Duration responseTimeout,
		@NotNull @Positive Integer maxOutputTokens,
		@NotNull @PositiveOrZero Float temperature,
		@NotBlank String thinkingLevel,
		@NotNull @DecimalMin("0.2") @DecimalMax("5.0") Double videoFps
) {
}
