package com.daesabu.meongcoach.graduation.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GraduationReviewRequest(@NotBlank String result, @Size(max = 500) String note) {
}
