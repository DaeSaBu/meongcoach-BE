package com.daesabu.meongcoach.llm.adapter.integration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.llm.gemini")
public record GeminiProperties(
		@NotBlank String apiKey,
		@NotBlank String model,
		@NotBlank String videoModel,
		@NotNull Duration responseTimeout,
		@NotNull @Positive Integer maxOutputTokens,
		@NotNull @PositiveOrZero Float temperature,
		@NotBlank String thinkingLevel,
		@NotNull @DecimalMin("0.2") @DecimalMax("5.0") Double videoFps
) {

	private static final String SAMPLING_IGNORING_MODEL_PREFIX = "gemini-4";

	public Optional<Float> temperatureFor(String model) {
		if (model.startsWith(SAMPLING_IGNORING_MODEL_PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(temperature);
	}
}
