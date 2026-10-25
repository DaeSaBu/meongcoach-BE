package com.daesabu.meongcoach.ai.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.ai.evolink")
public record EvoLinkConnectionProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotNull Duration responseTimeout
) {
}
