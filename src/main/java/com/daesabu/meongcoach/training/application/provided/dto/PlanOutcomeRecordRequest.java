package com.daesabu.meongcoach.training.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanOutcomeRecordRequest(@NotBlank String outcome) {
}
