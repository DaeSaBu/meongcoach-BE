package com.daesabu.meongcoach.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.google-cloud")
public record GoogleCloudProperties(
		@NotBlank String projectId,
		@NotBlank String serviceAccountKey
) {
}
