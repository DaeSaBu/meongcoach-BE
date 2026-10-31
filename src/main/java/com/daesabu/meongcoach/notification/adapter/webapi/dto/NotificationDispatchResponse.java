package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;

public record NotificationDispatchResponse(int fids, boolean duplicate) {
	public static NotificationDispatchResponse from(NotificationDispatchResult result) {
		return new NotificationDispatchResponse(result.fids(), result.duplicate());
	}
}
