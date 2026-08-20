package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import java.util.List;

/**
 * 커리큘럼 세부 조회 응답. 커리큘럼 정보와 소속 레슨 목록을 담는다.
 */
public record CurriculumDetailResponse(Long curriculumId, Long topicId, String curriculumTitle,
		int curriculumSortOrder, List<LessonResponse> lessons) {

	public static CurriculumDetailResponse from(CurriculumDetailResult result) {
		List<LessonResponse> lessons = result.lessons().stream()
				.map(LessonResponse::from)
				.toList();
		return new CurriculumDetailResponse(result.id(), result.topicId(), result.title(), result.sortOrder(), lessons);
	}
}
