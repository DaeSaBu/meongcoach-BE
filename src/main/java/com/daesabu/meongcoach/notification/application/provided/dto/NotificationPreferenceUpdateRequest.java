package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.NotificationPreferenceUpdateCommand;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record NotificationPreferenceUpdateRequest(
		@NotNull @Size(max = 24) List<@Valid @NotNull HourPreferenceRequest> hours) {

	public NotificationPreferenceUpdateCommand toCommand() {
		Map<Integer, Boolean> enabledByHour = new HashMap<>();
		for (HourPreferenceRequest hour : hours) {
			Boolean previous = enabledByHour.put(hour.hour(), hour.enabled());
			if (previous != null) {
				throw new InvalidNotificationHoursException();
			}
		}
		return new NotificationPreferenceUpdateCommand(enabledByHour);
	}
}
