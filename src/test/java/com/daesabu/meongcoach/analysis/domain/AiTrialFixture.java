package com.daesabu.meongcoach.analysis.domain;

import org.springframework.test.util.ReflectionTestUtils;

public final class AiTrialFixture {

	private AiTrialFixture() {
	}

	public static AiTrial used(Long userId, int usedCount) {
		return create(userId, AiTrial.DEFAULT_GRANTED_COUNT, usedCount);
	}

	public static AiTrial create(Long userId, int grantedCount, int usedCount) {
		AiTrial trial = new AiTrial();
		ReflectionTestUtils.setField(trial, "userId", userId);
		ReflectionTestUtils.setField(trial, "grantedCount", grantedCount);
		ReflectionTestUtils.setField(trial, "usedCount", usedCount);
		return trial;
	}
}
