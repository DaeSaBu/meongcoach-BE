package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatchResult;

public record EventNotificationDispatchResponse(int fids) {
	public static EventNotificationDispatchResponse from(EventNotificationDispatchResult result) {
		return new EventNotificationDispatchResponse(result.fids());
	}
}
