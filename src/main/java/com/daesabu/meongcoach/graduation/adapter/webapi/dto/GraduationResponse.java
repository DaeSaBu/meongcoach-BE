package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

public record GraduationResponse(Long topicId, String topicTitle, boolean eligible, String status, Long graduationId,
                                 int attemptCount, LatestSubmissionResponse latestSubmission, RewardResponse reward) {

	private static final String NOT_SUBMITTED = "NOT_SUBMITTED";

	public static GraduationResponse from(GraduationDetailResult result) {
		TopicGraduation graduation = result.graduation();
		GraduationSubmission latest = result.latestSubmission();
		LatestSubmissionResponse latestSubmission = LatestSubmissionResponse.of(latest, result.latestVideoUrl());
		RewardResponse reward = RewardResponse.from(graduation);
		if (graduation == null) {
			return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(), NOT_SUBMITTED,
					null, 0, latestSubmission, reward);
		}
		return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(),
				graduation.getStatus().name(), graduation.getId(), latest.getAttemptNo(), latestSubmission, reward);
	}
}
