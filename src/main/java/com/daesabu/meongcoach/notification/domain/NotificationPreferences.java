package com.daesabu.meongcoach.notification.domain;

import static org.springframework.util.Assert.state;

import java.util.ArrayList;
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

		return NotificationHour.ALLOWED_HOURS.stream()
				.map(hour -> NotificationPreference.create(userId, hour, NotificationHour.isDefaultEnabled(hour)))
				.toList();
	}

	public List<NotificationPreference> apply(NotificationPreferenceUpdateCommand command) {
		List<NotificationPreference> created = new ArrayList<>();
		for (HourPreference hourPreference : command.hours()) {
			int hour = hourPreference.hour();
			Optional<NotificationPreference> existing = find(hour);
			if (existing.isPresent()) {
				existing.get().change(hourPreference.enabled());
				continue;
			}
			created.add(NotificationPreference.create(userId, hour, hourPreference.enabled()));
		}
		return created;
	}

	public List<HourPreference> hourPreferences() {
		return NotificationHour.ALLOWED_HOURS.stream()
				.map(this::hourPreference)
				.toList();
	}

	private HourPreference hourPreference(int hour) {
		return find(hour)
				.map(NotificationPreference::toHourPreference)
				.orElseGet(() -> HourPreference.defaultOf(hour));
	}

	private Optional<NotificationPreference> find(int hour) {
		return preferences.stream()
				.filter(preference -> preference.getHour() == hour)
				.findFirst();
	}
}
