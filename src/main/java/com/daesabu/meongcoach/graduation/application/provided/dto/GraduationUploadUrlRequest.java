package com.daesabu.meongcoach.graduation.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GraduationUploadUrlRequest(@NotBlank String contentType, @NotNull Long fileSizeBytes) {
}
