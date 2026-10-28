package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.SlotPreference;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SlotPreferenceRequest(
		@NotBlank String slot,
		@NotNull Boolean enabled) {

	public SlotPreference toSlotPreference() {
		return new SlotPreference(NotificationSlot.from(slot), enabled);
	}
}
