package com.daesabu.meongcoach.media.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.storage.gcs")
public record GcsProperties(
		@NotBlank String bucket,
		@NotNull Duration uploadUrlValidity,
		@NotNull Duration downloadUrlValidity
) {
}
