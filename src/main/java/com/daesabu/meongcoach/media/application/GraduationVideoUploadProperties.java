package com.daesabu.meongcoach.media.application;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.media.graduation")
public record GraduationVideoUploadProperties(@Positive long maxFileSizeBytes) {
}
