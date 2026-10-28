package com.daesabu.meongcoach.notification.domain;

import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NotificationPreferences {

	private final Long userId;
	private final List<NotificationPreference> preferences;

	public NotificationPreferences(Long userId, List<NotificationPreference> preferences) {
		this.userId = userId;
		this.preferences = List.copyOf(preferences);
	}

	public boolean isEmpty() {
		return preferences.isEmpty();
	}

	public List<NotificationPreference> createDefaults() {
		state(isEmpty(), "이미 알림 설정이 있습니다");

		return Arrays.stream(NotificationSlot.values())
				.map(slot -> NotificationPreference.create(userId, slot, slot.isDefaultEnabled()))
				.toList();
	}

	public List<NotificationPreference> apply(NotificationPreferenceUpdateCommand command) {
		List<NotificationPreference> created = new ArrayList<>();
		for (NotificationSlot slot : NotificationSlot.values()) {
			boolean enabled = command.isEnabled(slot);
			Optional<NotificationPreference> existing = find(slot);
			if (existing.isPresent()) {
				existing.get().change(enabled);
				continue;
			}
			created.add(NotificationPreference.create(userId, slot, enabled));
		}
		return created;
	}

	public List<SlotPreference> slotPreferences() {
		return Arrays.stream(NotificationSlot.values())
				.map(this::slotPreference)
				.toList();
	}

	private SlotPreference slotPreference(NotificationSlot slot) {
		return find(slot)
				.map(NotificationPreference::toSlotPreference)
				.orElseGet(() -> SlotPreference.defaultOf(slot));
	}

	private Optional<NotificationPreference> find(NotificationSlot slot) {
		return preferences.stream()
				.filter(preference -> preference.getSlot() == slot)
				.findFirst();
	}
}
