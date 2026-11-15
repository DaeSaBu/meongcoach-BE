package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.ProgressStatus;

public record CurriculumResult(Curriculum curriculum, int completedLessons, ProgressStatus status, boolean locked) {

	public static CurriculumResult of(Curriculum curriculum, LessonProgresses progresses, boolean hasEntitlement) {
		int completedLessons = curriculum.countCompletedLessons(progresses);
		ProgressStatus status = curriculum.determineStatus(progresses);
		boolean locked = curriculum.isLocked(hasEntitlement);
		return new CurriculumResult(curriculum, completedLessons, status, locked);
	}
}
