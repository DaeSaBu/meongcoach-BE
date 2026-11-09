package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;

public record TopicCompletion(int completedLessons, ProgressStatus status) {

	public static TopicCompletion of(int totalLessons, int completedLessons) {
		ProgressStatus status = ProgressStatus.of(totalLessons, completedLessons);
		return new TopicCompletion(completedLessons, status);
	}
}
