package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import java.util.List;

public record NotificationTargetsResponse(int hour, List<NotificationTargetResponse> targets) {

	public static NotificationTargetsResponse of(int hour, List<NotificationTargetResult> results) {
		List<NotificationTargetResponse> targets = results.stream()
				.map(NotificationTargetResponse::from)
				.toList();
		return new NotificationTargetsResponse(hour, targets);
	}
}
