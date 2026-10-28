package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceResult;
import java.util.List;

public record NotificationPreferencesResponse(List<HourPreferenceResponse> hours) {

	public static NotificationPreferencesResponse from(List<NotificationPreferenceResult> preferences) {
		List<HourPreferenceResponse> hours = preferences.stream()
				.map(HourPreferenceResponse::from)
				.toList();
		return new NotificationPreferencesResponse(hours);
	}
}
