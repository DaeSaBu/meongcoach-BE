package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.RewardPolicy;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record RewardResponse(int days, Instant claimedAt, Instant expiresAt) {

	public static RewardResponse from(TopicGraduation graduation) {
		if (graduation == null) {
			return new RewardResponse(RewardPolicy.DAYS, null, null);
		}
		return new RewardResponse(RewardPolicy.DAYS, graduation.getRewardClaimedAt(), graduation.getRewardExpiresAt());
	}
}
