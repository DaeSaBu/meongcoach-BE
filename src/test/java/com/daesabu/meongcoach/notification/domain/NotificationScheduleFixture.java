package com.daesabu.meongcoach.notification.domain;

public final class NotificationScheduleFixture {

	private NotificationScheduleFixture() {
	}

	public static NotificationSchedule create(Long userId, int hour, boolean enabled) {
		return NotificationSchedule.create(userId, hour, enabled);
	}
}
