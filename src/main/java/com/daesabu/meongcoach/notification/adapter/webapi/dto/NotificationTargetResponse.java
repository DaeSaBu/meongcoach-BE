package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import java.util.List;

public record NotificationTargetResponse(Long userId, List<DeviceTokenResponse> tokens) {

	public static NotificationTargetResponse from(NotificationTargetResult result) {
		List<DeviceTokenResponse> tokens = result.tokens().stream()
				.map(DeviceTokenResponse::from)
				.toList();
		return new NotificationTargetResponse(result.userId(), tokens);
	}
}
