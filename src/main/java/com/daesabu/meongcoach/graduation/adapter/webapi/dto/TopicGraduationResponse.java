package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationReview;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionStatus;
import java.time.Instant;

public record TopicGraduationResponse(Long submissionId, GraduationSubmissionStatus status, String videoUrl,
                                      Instant submittedAt, Instant reviewedAt, GraduationReview review,
                                      String retakeImageUrl) {
	public static TopicGraduationResponse of(GraduationSubmission submission, String videoUrl) {
		return new TopicGraduationResponse(submission.getId(), submission.getStatus(), videoUrl,
				submission.getSubmittedAt(),
				submission.getReviewedAt(), submission.getReview(), submission.getRetakeImageUrl());
	}
}
