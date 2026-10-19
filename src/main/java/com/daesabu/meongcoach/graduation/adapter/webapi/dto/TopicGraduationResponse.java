package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record TopicGraduationResponse(Long graduationId, GraduationStatus status, int attemptNo, String videoUrl,
                                      Instant submittedAt, Instant reviewedAt, String reviewNote) {

	public static TopicGraduationResponse of(TopicGraduation graduation, String videoUrl) {
		return new TopicGraduationResponse(graduation.getId(), graduation.getStatus(), graduation.getAttemptNo(),
				videoUrl, graduation.getSubmittedAt(), graduation.getReviewedAt(), graduation.getReviewNote());
	}
}
