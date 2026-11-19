package com.daesabu.meongcoach.training.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record CurriculumProgressResultRequest(@NotBlank String result) {
}
