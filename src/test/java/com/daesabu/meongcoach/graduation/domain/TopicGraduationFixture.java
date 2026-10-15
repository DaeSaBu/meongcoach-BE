package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class TopicGraduationFixture {

	private TopicGraduationFixture() {
	}

	public static TopicGraduation create(Long id, Long userId, Long topicId, GraduationStatus status, Instant approvedAt,
	                                     Instant rewardClaimedAt, Instant rewardExpiresAt) {
		TopicGraduation graduation = TopicGraduation.start(userId, topicId);
		ReflectionTestUtils.setField(graduation, "id", id);
		ReflectionTestUtils.setField(graduation, "status", status);
		ReflectionTestUtils.setField(graduation, "approvedAt", approvedAt);
		ReflectionTestUtils.setField(graduation, "rewardClaimedAt", rewardClaimedAt);
		ReflectionTestUtils.setField(graduation, "rewardExpiresAt", rewardExpiresAt);
		return graduation;
	}
}
