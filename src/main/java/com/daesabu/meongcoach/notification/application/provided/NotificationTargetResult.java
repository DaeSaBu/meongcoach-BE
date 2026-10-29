package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceToken;

public record NotificationTargetResult(Long userId, String token, DevicePlatform platform) {

	public static NotificationTargetResult from(DeviceToken deviceToken) {
		return new NotificationTargetResult(deviceToken.getUserId(), deviceToken.getToken(), deviceToken.getPlatform());
	}
}
