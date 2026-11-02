package com.daesabu.meongcoach.notification.domain;

import java.util.List;

public class NotificationSchedules {

	private final List<NotificationSchedule> schedules;

	public NotificationSchedules(List<NotificationSchedule> schedules) {
		this.schedules = List.copyOf(schedules);
	}

	public List<NotificationSchedule> createMissingDefaults(Long userId) {
		List<NotificationTime> existingTimes = schedules.stream()
				.map(NotificationSchedule::time)
				.toList();
		return NotificationSchedule.createDefaults(userId).stream()
				.filter(schedule -> !existingTimes.contains(schedule.time()))
				.toList();
	}
}
