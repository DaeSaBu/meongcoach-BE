package com.daesabu.meongcoach.ai.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.ai.evolink")
public record EvoLinkProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotNull Duration responseTimeout,
		@NotBlank String model,
		@NotNull @Positive Integer maxTokens,
		@NotNull @PositiveOrZero Double temperature,
		@NotBlank String thinking
) {
}
