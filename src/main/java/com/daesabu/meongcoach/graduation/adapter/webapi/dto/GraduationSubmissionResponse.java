package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.time.Instant;

public record GraduationSubmissionResponse(Long graduationId, int attemptNo, Instant submittedAt) {

	public static GraduationSubmissionResponse from(TopicGraduation graduation) {
		return new GraduationSubmissionResponse(graduation.getId(), graduation.getAttemptNo(), graduation.getSubmittedAt());
	}
}
