package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record NotificationPreferenceUpdateCommand(List<HourPreference> hours) {

	public NotificationPreferenceUpdateCommand {
		hours = List.copyOf(hours);
		if (hours.size() != NotificationHour.ALLOWED_HOURS.size()) {
			throw new InvalidNotificationHoursException();
		}
		Set<Integer> distinctHours = hours.stream().map(HourPreference::hour).collect(Collectors.toSet());
		if (!distinctHours.equals(Set.copyOf(NotificationHour.ALLOWED_HOURS))) {
			throw new InvalidNotificationHoursException();
		}
	}

	public boolean isEnabled(int hour) {
		return enabledByHour().get(hour);
	}

	private Map<Integer, Boolean> enabledByHour() {
		Map<Integer, Boolean> enabledByHour = new HashMap<>();
		hours.forEach(preference -> enabledByHour.put(preference.hour(), preference.enabled()));
		return enabledByHour;
	}
}
