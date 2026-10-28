package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;

public record NotificationTargetResponse(Long userId, String token, DevicePlatform platform) {

	public static NotificationTargetResponse from(NotificationTargetResult result) {
		return new NotificationTargetResponse(result.userId(), result.token(), result.platform());
	}
}
