package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.List;
import java.util.Map;

public record CurriculumDetailResponse(Long curriculumId, String curriculumTitle, List<LessonResponse> lessons) {

	public static CurriculumDetailResponse from(CurriculumDetailResult result) {
		Curriculum curriculum = result.curriculum();
		Map<Long, Integer> completedCounts = result.completedCounts();
		List<LessonResponse> lessons = curriculum.getLessons().stream()
				.map(lesson -> LessonResponse.of(lesson, completedCounts.get(lesson.getId())))
				.toList();
		return new CurriculumDetailResponse(curriculum.getId(), curriculum.getTitle(), lessons);
	}
}
