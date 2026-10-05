package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;
import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;

public record CurriculumListResponse(Long topicId, String topicTitle, List<CurriculumResponse> curriculums) {

	public static CurriculumListResponse from(CurriculumListResult result) {
		Topic topic = result.topic();
		List<CurriculumResponse> curriculums = result.curriculums().stream()
				.map(curriculum -> CurriculumResponse.of(curriculum, result.completedLessonIds()))
				.toList();
		return new CurriculumListResponse(topic.getId(), topic.getTitle(), curriculums);
	}
}
