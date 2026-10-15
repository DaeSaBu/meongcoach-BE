package com.daesabu.meongcoach.plan.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanCompleteRequest(@NotBlank String outcome) {
}
