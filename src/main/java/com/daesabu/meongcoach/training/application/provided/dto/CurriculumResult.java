package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.ProgressStatus;

public record CurriculumResult(Curriculum curriculum, int completedLessons, ProgressStatus status) {

	public static CurriculumResult of(Curriculum curriculum, LessonProgresses progresses) {
		int completedLessons = curriculum.countCompletedLessons(progresses);
		ProgressStatus status = curriculum.determineStatus(progresses);
		return new CurriculumResult(curriculum, completedLessons, status);
	}
}
