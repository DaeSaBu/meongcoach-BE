package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotNull;

public record HourPreferenceRequest(
		@NotNull Integer hour,
		@NotNull Boolean enabled) {
}
