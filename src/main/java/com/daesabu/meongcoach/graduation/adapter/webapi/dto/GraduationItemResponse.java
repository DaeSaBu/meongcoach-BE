package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record GraduationItemResponse(Long graduationId, Long topicId, String topicTitle, GraduationStatus status,
                                     int attemptCount, Instant submittedAt, Instant reviewedAt, String reviewerNote,
                                     RewardResponse reward) {

	public static GraduationItemResponse from(GraduationSummaryResult result) {
		TopicGraduation graduation = result.graduation();
		GraduationSubmission latest = result.latestSubmission();
		return new GraduationItemResponse(graduation.getId(), graduation.getTopicId(), result.topicTitle(),
				graduation.getStatus(), latest.getAttemptNo(), latest.getSubmittedAt(), latest.getReviewedAt(),
				latest.getReviewerNote(), RewardResponse.from(graduation));
	}
}
