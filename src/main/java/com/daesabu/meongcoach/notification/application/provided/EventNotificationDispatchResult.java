package com.daesabu.meongcoach.notification.application.provided;

public record EventNotificationDispatchResult(int fids, int retryableFailures) {
	public EventNotificationDispatchResult(int fids) {
		this(fids, 0);
	}
}
