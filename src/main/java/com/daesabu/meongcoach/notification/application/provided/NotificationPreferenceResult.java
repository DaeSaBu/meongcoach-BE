package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.NotificationPreference;

public record NotificationPreferenceResult(int hour, boolean enabled) {

	public static NotificationPreferenceResult from(NotificationPreference preference) {
		return new NotificationPreferenceResult(preference.getHour(), preference.isEnabled());
	}
}
