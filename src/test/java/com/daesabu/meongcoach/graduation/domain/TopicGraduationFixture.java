package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class TopicGraduationFixture {

	private TopicGraduationFixture() {
	}

	public static TopicGraduation create(Long id, Long userId, Long topicId, GraduationStatus status, int attemptNo,
	                                     String videoObjectKey, Instant submittedAt, Instant reviewedAt, String reviewNote) {
		TopicGraduation graduation = TopicGraduation.start(userId, topicId, videoObjectKey, submittedAt);
		ReflectionTestUtils.setField(graduation, "id", id);
		ReflectionTestUtils.setField(graduation, "status", status);
		ReflectionTestUtils.setField(graduation, "attemptNo", attemptNo);
		if (status == GraduationStatus.APPROVED) {
			ReflectionTestUtils.setField(graduation, "approvedAt", reviewedAt);
		}
		return graduation;
	}

	public static GraduationSubmission submission(TopicGraduation graduation, Instant reviewedAt, String reviewNote) {
		GraduationSubmission submission = GraduationSubmission.from(graduation);
		ReflectionTestUtils.setField(submission, "status", GraduationSubmissionStatus.valueOf(graduation.getStatus().name()));
		ReflectionTestUtils.setField(submission, "reviewedAt", reviewedAt);
		ReflectionTestUtils.setField(submission, "reviewNote", reviewNote);
		return submission;
	}

	public static GraduationSubmission withRetakeGuide(GraduationSubmission submission, String retakeImageUrl,
	                                                  List<String> retakeInstructions) {
		ReflectionTestUtils.setField(submission, "retakeImageUrl", retakeImageUrl);
		ReflectionTestUtils.setField(submission, "retakeInstructions", new ArrayList<>(retakeInstructions));
		return submission;
	}
}
