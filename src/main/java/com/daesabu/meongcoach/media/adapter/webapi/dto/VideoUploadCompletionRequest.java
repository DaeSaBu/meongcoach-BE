package com.daesabu.meongcoach.media.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

public record VideoUploadCompletionRequest(@NotBlank String objectKey) {
}
