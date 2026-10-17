package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record GraduationResponse(Long topicId, String topicTitle, boolean eligible, String status, Long graduationId,
                                 int attemptNo, String videoUrl, Instant submittedAt, Instant reviewedAt, String reviewNote,
                                 Instant rewardExpiresAt) {

	private static final String NOT_SUBMITTED = "NOT_SUBMITTED";

	public static GraduationResponse from(GraduationDetailResult result) {
		TopicGraduation graduation = result.graduation();
		if (graduation == null) {
			return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(), NOT_SUBMITTED,
					null, 0, null, null, null, null, null);
		}
		return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(),
				graduation.getStatus().name(), graduation.getId(), graduation.getAttemptNo(), result.videoUrl(),
				graduation.getSubmittedAt(), graduation.getReviewedAt(), graduation.getReviewNote(),
				graduation.getRewardExpiresAt());
	}
}
