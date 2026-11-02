package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.ScheduledNotificationDispatchResult;

public record ScheduledNotificationDispatchResponse(int fids, boolean duplicate) {
	public static ScheduledNotificationDispatchResponse from(ScheduledNotificationDispatchResult result) {
		return new ScheduledNotificationDispatchResponse(result.fids(), result.duplicate());
	}
}
