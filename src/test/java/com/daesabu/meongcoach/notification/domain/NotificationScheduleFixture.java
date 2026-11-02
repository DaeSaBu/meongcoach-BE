package com.daesabu.meongcoach.notification.domain;

public final class NotificationScheduleFixture {

	private NotificationScheduleFixture() {
	}

	public static NotificationSchedule create(Long userId, int hour, boolean enabled) {
		return create(userId, hour, 0, enabled);
	}

	public static NotificationSchedule create(Long userId, int hour, int minute, boolean enabled) {
		return NotificationSchedule.create(userId, new NotificationTime(hour, minute), enabled);
	}
}
