package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.List;

public record NotificationTargetsResponse(NotificationSlot slot, List<NotificationTargetResponse> targets) {

	public static NotificationTargetsResponse of(NotificationSlot slot, List<NotificationTargetResult> results) {
		List<NotificationTargetResponse> targets = results.stream()
				.map(NotificationTargetResponse::from)
				.toList();
		return new NotificationTargetsResponse(slot, targets);
	}
}
