package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.HourPreference;
import jakarta.validation.constraints.NotNull;

public record HourPreferenceRequest(
		@NotNull Integer hour,
		@NotNull Boolean enabled) {

	public HourPreference toHourPreference() {
		return new HourPreference(hour, enabled);
	}
}
