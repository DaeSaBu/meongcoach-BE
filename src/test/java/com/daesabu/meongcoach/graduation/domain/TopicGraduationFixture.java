package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class TopicGraduationFixture {

	private TopicGraduationFixture() {
	}

	public static TopicGraduation create(Long id, Long userId, Long topicId, GraduationStatus status, int attemptNo,
	                                     String videoObjectKey, Instant submittedAt, Instant reviewedAt, String reviewNote,
	                                     Instant rewardExpiresAt) {
		TopicGraduation graduation = TopicGraduation.start(userId, topicId, videoObjectKey, submittedAt);
		ReflectionTestUtils.setField(graduation, "id", id);
		ReflectionTestUtils.setField(graduation, "status", status);
		ReflectionTestUtils.setField(graduation, "attemptNo", attemptNo);
		ReflectionTestUtils.setField(graduation, "reviewedAt", reviewedAt);
		ReflectionTestUtils.setField(graduation, "reviewNote", reviewNote);
		ReflectionTestUtils.setField(graduation, "rewardExpiresAt", rewardExpiresAt);
		if (status == GraduationStatus.APPROVED) {
			ReflectionTestUtils.setField(graduation, "approvedAt", reviewedAt);
		}
		return graduation;
	}
}
