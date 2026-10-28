package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationSlotsException;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record NotificationPreferenceUpdateCommand(List<SlotPreference> slots) {

	public NotificationPreferenceUpdateCommand {
		slots = List.copyOf(slots);
		if (slots.size() != NotificationSlot.values().length) {
			throw new InvalidNotificationSlotsException();
		}
		long distinctSlots = slots.stream().map(SlotPreference::slot).distinct().count();
		if (distinctSlots != NotificationSlot.values().length) {
			throw new InvalidNotificationSlotsException();
		}
	}

	public boolean isEnabled(NotificationSlot slot) {
		return enabledBySlot().get(slot);
	}

	private Map<NotificationSlot, Boolean> enabledBySlot() {
		Map<NotificationSlot, Boolean> enabledBySlot = new EnumMap<>(NotificationSlot.class);
		slots.forEach(preference -> enabledBySlot.put(preference.slot(), preference.enabled()));
		return enabledBySlot;
	}
}
