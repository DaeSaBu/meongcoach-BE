package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import java.util.List;
import java.util.Map;

public record CurriculumDetailResponse(Long curriculumId, Long topicId, String curriculumTitle,
		int curriculumSortOrder, List<LessonResponse> lessons) {

	public static CurriculumDetailResponse from(CurriculumDetailResult result) {
		Curriculum curriculum = result.curriculum();
		Map<Long, Integer> completedCounts = result.completedCounts();
		List<LessonResponse> lessons = curriculum.getLessons().stream()
				.map(lesson -> LessonResponse.of(lesson, completedCounts.get(lesson.getId())))
				.toList();
		return new CurriculumDetailResponse(curriculum.getId(), curriculum.getTopic().getId(), curriculum.getTitle(),
				curriculum.getSortOrder(), lessons);
	}
}
