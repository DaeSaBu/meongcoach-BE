package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import java.util.List;

public record CurriculumDetailResponse(Long curriculumId, String curriculumTitle, ExpertResponse expert,
		List<LessonResponse> lessons) {

	public static CurriculumDetailResponse from(CurriculumDetailResult result) {
		Curriculum curriculum = result.curriculum();
		LessonProgresses progresses = result.progresses();
		ExpertResponse expert = curriculum.findExpert()
				.map(ExpertResponse::from)
				.orElse(null);
		List<LessonResponse> lessons = curriculum.getLessons().stream()
				.map(lesson -> LessonResponse.of(lesson, progresses.completedCount(lesson.getId())))
				.toList();
		return new CurriculumDetailResponse(curriculum.getId(), curriculum.getTitle(), expert, lessons);
	}
}
