package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import java.util.List;

public record NotificationSchedulesResponse(List<HourScheduleResponse> hours) {

	public static NotificationSchedulesResponse from(List<NotificationScheduleResult> schedules) {
		List<HourScheduleResponse> hours = schedules.stream()
				.map(HourScheduleResponse::from)
				.toList();
		return new NotificationSchedulesResponse(hours);
	}
}
