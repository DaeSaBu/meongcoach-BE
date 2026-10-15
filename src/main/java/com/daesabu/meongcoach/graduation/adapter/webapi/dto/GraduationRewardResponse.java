package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.RewardPolicy;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record GraduationRewardResponse(int days, Instant claimedAt, Instant expiresAt) {

	public static GraduationRewardResponse from(TopicGraduation graduation) {
		return new GraduationRewardResponse(RewardPolicy.DAYS, graduation.getRewardClaimedAt(),
				graduation.getRewardExpiresAt());
	}
}
