package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import java.util.List;

/**
 * 커리큘럼 리스트 조회 응답. 커리큘럼 탭에 표시 중인 토픽과 그 커리큘럼을 담는다.
 */
public record CurriculumListResponse(Long topicId, String topicTitle, List<CurriculumResponse> curriculums) {

	public static CurriculumListResponse from(CurriculumListResult result) {
		List<CurriculumResponse> curriculums = result.curriculums().stream()
				.map(CurriculumResponse::from)
				.toList();
		return new CurriculumListResponse(result.topicId(), result.topicTitle(), curriculums);
	}
}
