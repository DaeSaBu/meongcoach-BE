package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationTargetException;
import java.util.List;

public record NotificationTarget(boolean all, List<Long> userIds) {

	private static final String ALL = "ALL";
	private static final String USERS = "USERS";
	private static final int MAX_USERS = 1_000;

	public static NotificationTarget of(String kind, List<Long> userIds) {
		boolean hasUsers = userIds != null && !userIds.isEmpty();
		if (ALL.equals(kind) && !hasUsers) {
			return new NotificationTarget(true, List.of());
		}
		if (USERS.equals(kind) && hasUsers && userIds.size() <= MAX_USERS) {
			return new NotificationTarget(false, List.copyOf(userIds));
		}
		throw new InvalidNotificationTargetException();
	}
}
