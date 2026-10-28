package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.DeviceToken;
import java.util.List;

public record NotificationTargetResult(Long userId, List<DeviceTokenResult> tokens) {

	public static NotificationTargetResult of(Long userId, List<DeviceToken> deviceTokens) {
		List<DeviceTokenResult> tokens = deviceTokens.stream()
				.map(DeviceTokenResult::from)
				.toList();
		return new NotificationTargetResult(userId, tokens);
	}
}
