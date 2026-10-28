package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.domain.HourPreference;

public record HourPreferenceResponse(int hour, boolean enabled) {

	public static HourPreferenceResponse from(HourPreference hourPreference) {
		return new HourPreferenceResponse(hourPreference.hour(), hourPreference.enabled());
	}
}
