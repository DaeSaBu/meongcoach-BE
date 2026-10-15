package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record AdminSubmissionResponse(Long submissionId, Long graduationId, Long userId, Long topicId, String topicTitle,
                                      int attemptNo, SubmissionResult result, Instant submittedAt, String videoUrl) {

	public static AdminSubmissionResponse from(AdminSubmissionResult result) {
		GraduationSubmission submission = result.submission();
		TopicGraduation graduation = result.graduation();
		return new AdminSubmissionResponse(submission.getId(), graduation.getId(), graduation.getUserId(),
				graduation.getTopicId(), result.topicTitle(), submission.getAttemptNo(), submission.getResult(),
				submission.getSubmittedAt(), result.videoUrl());
	}
}
