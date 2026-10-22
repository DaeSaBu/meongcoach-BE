package com.daesabu.meongcoach.training.domain;

public record CurriculumCompletion(int completedLessons, ProgressStatus status) {

	public static CurriculumCompletion of(int totalLessons, int completedLessons) {
		ProgressStatus status = ProgressStatus.of(totalLessons, completedLessons);
		return new CurriculumCompletion(completedLessons, status);
	}
}
