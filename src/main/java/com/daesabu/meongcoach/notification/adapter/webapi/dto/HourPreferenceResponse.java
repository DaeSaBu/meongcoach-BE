package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationPreferenceResult;

public record HourPreferenceResponse(int hour, boolean enabled) {

	public static HourPreferenceResponse from(NotificationPreferenceResult preference) {
		return new HourPreferenceResponse(preference.hour(), preference.enabled());
	}
}
