package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import java.time.Instant;

public record AdminReviewResponse(Long submissionId, SubmissionResult result, Instant reviewedAt, String reviewerNote) {

	public static AdminReviewResponse from(GraduationSubmission submission) {
		return new AdminReviewResponse(submission.getId(), submission.getResult(), submission.getReviewedAt(),
				submission.getReviewerNote());
	}
}
