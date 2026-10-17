package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record GraduationItemResponse(Long graduationId, Long topicId, String topicTitle, String status, int attemptNo,
                                     Instant submittedAt, Instant reviewedAt, String reviewNote, Instant rewardExpiresAt) {

	public static GraduationItemResponse from(GraduationSummaryResult result) {
		TopicGraduation graduation = result.graduation();
		return new GraduationItemResponse(graduation.getId(), graduation.getTopicId(), result.topicTitle(),
				graduation.getStatus().name(), graduation.getAttemptNo(), graduation.getSubmittedAt(),
				graduation.getReviewedAt(), graduation.getReviewNote(), graduation.getRewardExpiresAt());
	}
}
