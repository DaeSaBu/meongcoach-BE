package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;
import java.util.List;

public record TopicGraduationResponse(Long graduationId, GraduationStatus status, String videoUrl,
                                      Instant submittedAt, Instant reviewedAt, String reviewNote,
                                      List<String> retakeInstructions, String retakeImageUrl) {

	public static TopicGraduationResponse of(TopicGraduation graduation, GraduationSubmission submission,
	                                         String videoUrl) {
		return new TopicGraduationResponse(graduation.getId(), graduation.getStatus(),
				videoUrl, graduation.getSubmittedAt(), submission.getReviewedAt(), submission.getReviewNote(),
				List.copyOf(submission.getRetakeInstructions()), submission.getRetakeImageUrl());
	}
}
