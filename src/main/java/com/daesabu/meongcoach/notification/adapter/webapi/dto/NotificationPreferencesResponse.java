package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.domain.HourPreference;
import java.util.List;

public record NotificationPreferencesResponse(List<HourPreferenceResponse> hours) {

	public static NotificationPreferencesResponse from(List<HourPreference> hourPreferences) {
		List<HourPreferenceResponse> hours = hourPreferences.stream()
				.map(HourPreferenceResponse::from)
				.toList();
		return new NotificationPreferencesResponse(hours);
	}
}
