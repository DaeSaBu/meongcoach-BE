package com.daesabu.meongcoach.notification.domain;

public final class NotificationPreferenceFixture {

	private NotificationPreferenceFixture() {
	}

	public static NotificationPreference create(Long userId, int hour, boolean enabled) {
		return NotificationPreference.create(userId, hour, enabled);
	}
}
