package com.daesabu.meongcoach.training.domain.shared;

public enum CurriculumStatus {
	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED;

	public static CurriculumStatus of(int totalLessons, int completedLessons) {
		if (totalLessons <= 0 || completedLessons <= 0) {
			return NOT_STARTED;
		}
		if (completedLessons < totalLessons) {
			return IN_PROGRESS;
		}
		return COMPLETED;
	}
}
