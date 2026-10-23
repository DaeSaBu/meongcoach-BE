package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCompletion;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import java.util.List;

public record PlanCurriculumResult(Long id, String title, String description, int totalLessons,
		int completedLessons, String status, boolean locked, List<PlanLessonResult> lessons) {

	public static PlanCurriculumResult of(Curriculum curriculum, LessonProgresses progresses, boolean hasEntitlement) {
		CurriculumCompletion completion = curriculum.completion(progresses);
		List<PlanLessonResult> lessons = curriculum.getLessons().stream()
				.map(lesson -> new PlanLessonResult(lesson.getId(), lesson.getTitle(),
						progresses.completedCount(lesson.getId()) > 0))
				.toList();
		return new PlanCurriculumResult(curriculum.getId(), curriculum.getTitle(), curriculum.getDescription(),
				curriculum.getLessonsSize(), completion.completedLessons(), completion.status().name(),
				curriculum.isLocked(hasEntitlement), lessons);
	}
}
