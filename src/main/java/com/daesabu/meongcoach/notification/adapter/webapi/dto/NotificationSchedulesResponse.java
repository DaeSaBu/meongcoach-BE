package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationScheduleResult;
import java.util.List;

public record NotificationSchedulesResponse(List<NotificationScheduleResponse> schedules) {

	public static NotificationSchedulesResponse from(List<NotificationScheduleResult> results) {
		List<NotificationScheduleResponse> schedules = results.stream()
				.map(NotificationScheduleResponse::from)
				.toList();
		return new NotificationSchedulesResponse(schedules);
	}
}
