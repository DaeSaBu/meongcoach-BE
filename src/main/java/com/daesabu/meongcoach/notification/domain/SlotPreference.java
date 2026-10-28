package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;

public record SlotPreference(NotificationSlot slot, boolean enabled) {

	static SlotPreference defaultOf(NotificationSlot slot) {
		return new SlotPreference(slot, slot.isDefaultEnabled());
	}
}
