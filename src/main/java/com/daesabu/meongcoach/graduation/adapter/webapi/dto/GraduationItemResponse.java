package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.domain.GraduationReview;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.time.Instant;
import java.util.List;

public record GraduationItemResponse(Long submissionId, Long topicId, String topicTitle, String status,
                                     Instant submittedAt, Instant reviewedAt,
                                     GraduationReview review) {

	public static GraduationItemResponse from(GraduationSummaryResult result) {
		GraduationSubmission graduation = result.submission();
		if (graduation == null) {
			GraduationReview review = new GraduationReview(null, List.of());
			return new GraduationItemResponse(null, result.topic().id(), result.topic().title(), "NOT_SUBMITTED",
					null, null, review);
		}
		return new GraduationItemResponse(graduation.getId(), graduation.getTopicId(), result.topic().title(),
				graduation.getStatus().name(), graduation.getSubmittedAt(),
				result.submission().getReviewedAt(), result.submission().getReview());
	}
}
