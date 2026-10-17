package com.daesabu.meongcoach.graduation.domain;

import java.time.Duration;
import java.time.Instant;

public final class RewardPolicy {

	public static final int DAYS = 3;

	private static final Duration REWARD_DURATION = Duration.ofDays(DAYS);

	private RewardPolicy() {
	}

	public static Instant expiresAt(Instant now, Instant previousExpiresAt) {
		Instant base = now;
		if (previousExpiresAt != null && previousExpiresAt.isAfter(now)) {
			base = previousExpiresAt;
		}
		return base.plus(REWARD_DURATION);
	}
}
