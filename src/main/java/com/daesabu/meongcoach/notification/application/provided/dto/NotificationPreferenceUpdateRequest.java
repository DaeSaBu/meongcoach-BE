package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.HourPreference;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceUpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NotificationPreferenceUpdateRequest(
		@NotNull @Size(max = 24) List<@Valid @NotNull HourPreferenceRequest> hours) {

	public NotificationPreferenceUpdateCommand toCommand() {
		List<HourPreference> hourPreferences = hours.stream()
				.map(HourPreferenceRequest::toHourPreference)
				.toList();
		return new NotificationPreferenceUpdateCommand(hourPreferences);
	}
}
