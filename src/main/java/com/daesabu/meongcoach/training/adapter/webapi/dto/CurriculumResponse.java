package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import java.util.Set;

public record CurriculumResponse(Long curriculumId, String curriculumTitle, int totalLessons, int completedLessons,
		CurriculumStatus status, boolean locked) {

	public static CurriculumResponse of(Curriculum curriculum, Set<Long> completedLessonIds, boolean hasEntitlement) {
		int totalLessons = curriculum.getLessonsSize();
		int completedLessons = curriculum.countCompletedLessons(completedLessonIds);
		CurriculumStatus status = curriculum.statusOf(completedLessonIds);
		boolean locked = curriculum.isLocked(hasEntitlement);
		return new CurriculumResponse(curriculum.getId(), curriculum.getTitle(), totalLessons, completedLessons,
				status, locked);
	}
}
