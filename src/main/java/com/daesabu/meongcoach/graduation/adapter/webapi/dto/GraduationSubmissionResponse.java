package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.time.Instant;

public record GraduationSubmissionResponse(Long submissionId, Instant submittedAt) {

	public static GraduationSubmissionResponse from(GraduationSubmission graduation) {
		return new GraduationSubmissionResponse(graduation.getId(), graduation.getSubmittedAt());
	}
}
