package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import java.time.Instant;

public record GraduationSubmissionResponse(Long submissionId, int attemptNo, SubmissionResult result, Instant submittedAt) {

	public static GraduationSubmissionResponse from(GraduationSubmission submission) {
		return new GraduationSubmissionResponse(submission.getId(), submission.getAttemptNo(), submission.getResult(),
				submission.getSubmittedAt());
	}
}
