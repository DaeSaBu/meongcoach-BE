package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class GraduationSubmissionFixture {

	private GraduationSubmissionFixture() {
	}

	public static GraduationSubmission create(Long id, Long graduationId, int attemptNo, String videoObjectKey,
	                                          SubmissionResult result, Instant submittedAt, Instant reviewedAt,
	                                          String reviewerNote) {
		GraduationSubmission submission = new GraduationSubmission();
		ReflectionTestUtils.setField(submission, "id", id);
		ReflectionTestUtils.setField(submission, "graduationId", graduationId);
		ReflectionTestUtils.setField(submission, "attemptNo", attemptNo);
		ReflectionTestUtils.setField(submission, "videoObjectKey", videoObjectKey);
		ReflectionTestUtils.setField(submission, "result", result);
		ReflectionTestUtils.setField(submission, "submittedAt", submittedAt);
		ReflectionTestUtils.setField(submission, "reviewedAt", reviewedAt);
		ReflectionTestUtils.setField(submission, "reviewerNote", reviewerNote);
		return submission;
	}
}
