package com.daesabu.meongcoach.notification.domain;

import java.util.List;

public class NotificationEventSettings {

	private final List<NotificationEventSetting> settings;

	public NotificationEventSettings(List<NotificationEventSetting> settings) {
		this.settings = List.copyOf(settings);
	}

	public List<NotificationEventSetting> createMissingDefaults(Long userId) {
		List<NotificationEventType> existingTypes = settings.stream()
				.map(NotificationEventSetting::getType)
				.toList();
		return NotificationEventSetting.createDefaults(userId).stream()
				.filter(setting -> !existingTypes.contains(setting.getType()))
				.toList();
	}
}
