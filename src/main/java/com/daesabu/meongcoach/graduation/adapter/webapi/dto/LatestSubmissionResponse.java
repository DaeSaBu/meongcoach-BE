package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import java.time.Instant;

public record LatestSubmissionResponse(Long submissionId, int attemptNo, SubmissionResult result, Instant submittedAt,
                                       Instant reviewedAt, String reviewerNote, String videoUrl) {

	public static LatestSubmissionResponse of(GraduationSubmission submission, String videoUrl) {
		if (submission == null) {
			return null;
		}
		return new LatestSubmissionResponse(submission.getId(), submission.getAttemptNo(), submission.getResult(),
				submission.getSubmittedAt(), submission.getReviewedAt(), submission.getReviewerNote(), videoUrl);
	}
}
