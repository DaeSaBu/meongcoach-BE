package com.daesabu.meongcoach.notification.domain;

public record HourPreference(int hour, boolean enabled) {

	public HourPreference {
		NotificationHour.validate(hour);
	}

	static HourPreference defaultOf(int hour) {
		return new HourPreference(hour, NotificationHour.isDefaultEnabled(hour));
	}
}
