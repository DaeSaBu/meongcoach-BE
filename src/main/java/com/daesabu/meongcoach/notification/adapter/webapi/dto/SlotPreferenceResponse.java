package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.domain.SlotPreference;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;

public record SlotPreferenceResponse(NotificationSlot slot, boolean enabled) {

	public static SlotPreferenceResponse from(SlotPreference slotPreference) {
		return new SlotPreferenceResponse(slotPreference.slot(), slotPreference.enabled());
	}
}
