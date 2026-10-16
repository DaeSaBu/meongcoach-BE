package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record AdminReviewResponse(Long graduationId, GraduationStatus status, Instant reviewedAt, String reviewNote,
                                  Instant rewardExpiresAt) {

	public static AdminReviewResponse from(TopicGraduation graduation) {
		return new AdminReviewResponse(graduation.getId(), graduation.getStatus(), graduation.getReviewedAt(),
				graduation.getReviewNote(), graduation.getRewardExpiresAt());
	}
}
