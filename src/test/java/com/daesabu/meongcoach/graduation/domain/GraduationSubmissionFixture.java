package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class GraduationSubmissionFixture {
	private GraduationSubmissionFixture() {
	}

	public static GraduationSubmission create(Long id, Long userId, Long topicId, GraduationSubmissionStatus status,
	                                          String videoObjectKey, Instant submittedAt, Instant reviewedAt,
	                                          String reviewNote) {
		GraduationSubmission submission = GraduationSubmission.create(
				new GraduationSubmissionCommand(userId, topicId, videoObjectKey, submittedAt));
		ReflectionTestUtils.setField(submission, "id", id);
		ReflectionTestUtils.setField(submission, "status", status);
		ReflectionTestUtils.setField(submission, "reviewedAt", reviewedAt);
		ReflectionTestUtils.setField(submission, "review", new GraduationReview(reviewNote, List.of()));
		return submission;
	}

	public static GraduationSubmission submission(GraduationSubmission submission, Instant reviewedAt,
	                                              String reviewNote) {
		return submission;
	}

	public static GraduationSubmission withRetakeGuide(GraduationSubmission submission, String retakeImageUrl,
	                                                   List<String> retakeInstructions) {
		ReflectionTestUtils.setField(submission, "retakeImageUrl", retakeImageUrl);
		ReflectionTestUtils.setField(submission, "review",
				new GraduationReview(submission.getReview().note(), retakeInstructions));
		return submission;
	}
}
