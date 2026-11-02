package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;

public record NotificationEventSettingResult(NotificationEventType type, boolean enabled) {

	public static NotificationEventSettingResult from(NotificationEventSetting setting) {
		return new NotificationEventSettingResult(setting.getType(), setting.isEnabled());
	}
}
