package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.NotificationPreferenceUpdateCommand;
import com.daesabu.meongcoach.notification.domain.SlotPreference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NotificationPreferenceUpdateRequest(
		@NotNull @Size(max = 10) List<@Valid @NotNull SlotPreferenceRequest> slots) {

	public NotificationPreferenceUpdateCommand toCommand() {
		List<SlotPreference> slotPreferences = slots.stream()
				.map(SlotPreferenceRequest::toSlotPreference)
				.toList();
		return new NotificationPreferenceUpdateCommand(slotPreferences);
	}
}
