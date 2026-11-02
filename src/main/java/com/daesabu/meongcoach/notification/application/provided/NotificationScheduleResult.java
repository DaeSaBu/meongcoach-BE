package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.NotificationSchedule;

public record NotificationScheduleResult(int hour, int minute, boolean enabled) {

	public static NotificationScheduleResult from(NotificationSchedule schedule) {
		return new NotificationScheduleResult(schedule.getHour(), schedule.getMinute(), schedule.isEnabled());
	}
}
