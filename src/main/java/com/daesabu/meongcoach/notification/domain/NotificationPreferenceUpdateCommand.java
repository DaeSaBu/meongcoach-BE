package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import java.util.Map;
import java.util.Set;

public record NotificationPreferenceUpdateCommand(Map<Integer, Boolean> enabledByHour) {

	public NotificationPreferenceUpdateCommand {
		enabledByHour = Map.copyOf(enabledByHour);
		enabledByHour.keySet().forEach(NotificationHour::validate);
		if (!enabledByHour.keySet().equals(Set.copyOf(NotificationHour.ALLOWED_HOURS))) {
			throw new InvalidNotificationHoursException();
		}
	}

	public boolean enabledFor(int hour) {
		return enabledByHour.get(hour);
	}
}
