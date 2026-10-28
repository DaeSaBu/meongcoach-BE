package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceToken;

public record DeviceTokenResult(String token, DevicePlatform platform) {

	public static DeviceTokenResult from(DeviceToken deviceToken) {
		return new DeviceTokenResult(deviceToken.getToken(), deviceToken.getPlatform());
	}
}
