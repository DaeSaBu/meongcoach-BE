package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;

public record HourScheduleResponse(int hour, boolean enabled) {

	public static HourScheduleResponse from(NotificationScheduleResult schedule) {
		return new HourScheduleResponse(schedule.hour(), schedule.enabled());
	}
}
