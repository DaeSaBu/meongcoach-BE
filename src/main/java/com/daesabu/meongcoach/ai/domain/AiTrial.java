package com.daesabu.meongcoach.ai.domain;

public record AiTrial(int usedCount) {

	public static final int MAX_COUNT = 3;

	public static AiTrial of(long completedCount) {
		return new AiTrial(Math.toIntExact(completedCount));
	}

	public int maxCount() {
		return MAX_COUNT;
	}

	public int remainingCount() {
		return Math.max(0, MAX_COUNT - usedCount);
	}

	public boolean isAvailable() {
		return usedCount < MAX_COUNT;
	}
}
