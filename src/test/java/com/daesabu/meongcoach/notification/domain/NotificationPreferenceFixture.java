package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;

public final class NotificationPreferenceFixture {

	private NotificationPreferenceFixture() {
	}

	public static NotificationPreference create(Long userId, NotificationSlot slot, boolean enabled) {
		return NotificationPreference.create(userId, slot, enabled);
	}
}
