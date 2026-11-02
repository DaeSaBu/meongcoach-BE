package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;

public record NotificationScheduleResponse(int hour, int minute, boolean enabled) {

	public static NotificationScheduleResponse from(NotificationScheduleResult schedule) {
		return new NotificationScheduleResponse(schedule.hour(), schedule.minute(), schedule.enabled());
	}
}
