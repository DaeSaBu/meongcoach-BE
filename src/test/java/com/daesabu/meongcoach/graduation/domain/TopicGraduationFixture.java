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
		GraduationSubmission submission = GraduationSubmission.record(graduation);
		ReflectionTestUtils.setField(submission, "status", GraduationSubmissionStatus.valueOf(status.name()));
		ReflectionTestUtils.setField(submission, "reviewedAt", reviewedAt);
		ReflectionTestUtils.setField(submission, "reviewNote", reviewNote);
		graduation.useSubmission(submission);
		if (status == GraduationStatus.APPROVED) {
			ReflectionTestUtils.setField(graduation, "approvedAt", reviewedAt);
		}
		return graduation;
	}

	public static TopicGraduation withRetakeGuide(TopicGraduation graduation, String retakeFramingIconCode, List<String> retakeInstructions) {
		ReflectionTestUtils.setField(graduation.getCurrentSubmission(), "retakeFramingIconCode", retakeFramingIconCode);
		ReflectionTestUtils.setField(graduation.getCurrentSubmission(), "retakeInstructions", new ArrayList<>(retakeInstructions));
		return graduation;
	}
}
