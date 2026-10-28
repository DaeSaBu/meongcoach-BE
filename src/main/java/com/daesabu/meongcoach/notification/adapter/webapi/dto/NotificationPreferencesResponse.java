package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.domain.SlotPreference;
import java.util.List;

public record NotificationPreferencesResponse(List<SlotPreferenceResponse> slots) {

	public static NotificationPreferencesResponse from(List<SlotPreference> slotPreferences) {
		List<SlotPreferenceResponse> slots = slotPreferences.stream()
				.map(SlotPreferenceResponse::from)
				.toList();
		return new NotificationPreferencesResponse(slots);
	}
}
