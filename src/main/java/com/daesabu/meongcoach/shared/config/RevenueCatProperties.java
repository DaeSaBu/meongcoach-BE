package com.daesabu.meongcoach.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.revenuecat")
public record RevenueCatProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotBlank String projectId
) {
}
