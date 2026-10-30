package com.daesabu.meongcoach.notification.domain;

import java.util.List;
import java.util.Objects;

public class NotificationDeliveryResults {
	private final List<NotificationDeliveryResult> results;

	public NotificationDeliveryResults(List<NotificationDeliveryResult> results) {
		this.results = List.copyOf(results);
	}

	public List<String> invalidFids() {
		return results.stream().filter(NotificationDeliveryResult::invalid)
				.map(NotificationDeliveryResult::fid).toList();
	}

	public void throwIfFailed() {
		results.stream().map(NotificationDeliveryResult::failure).filter(Objects::nonNull)
				.findFirst().ifPresent(failure -> { throw failure; });
	}
}
