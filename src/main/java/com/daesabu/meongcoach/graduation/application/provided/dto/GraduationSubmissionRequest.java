package com.daesabu.meongcoach.graduation.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record GraduationSubmissionRequest(@NotBlank String objectKey) {
}
