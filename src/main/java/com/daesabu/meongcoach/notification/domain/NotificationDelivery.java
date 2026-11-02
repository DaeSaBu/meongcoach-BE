package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import java.util.stream.Stream;

public record NotificationDelivery(int deliveredCount, int failedCount, List<String> unreachableFids) {

	public NotificationDelivery {
		unreachableFids = List.copyOf(unreachableFids);
	}

	public static NotificationDelivery none() {
		return new NotificationDelivery(0, 0, List.of());
	}

	public NotificationDelivery plus(NotificationDelivery other) {
		int delivered = deliveredCount + other.deliveredCount;
		int failed = failedCount + other.failedCount;
		List<String> unreachable = Stream.concat(unreachableFids.stream(), other.unreachableFids.stream()).toList();
		return new NotificationDelivery(delivered, failed, unreachable);
	}

	public boolean hasUnreachable() {
		return !unreachableFids.isEmpty();
	}

	public void requireDelivered() {
		if (deliveredCount == 0 && failedCount > 0) {
			throw new NotificationDeliveryException();
		}
	}
}
