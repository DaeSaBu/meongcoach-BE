package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;

/**
 * 커리큘럼 응답. 전체 레슨 수와 사용자가 완료한 레슨 수, 진행 상태를 담는다.
 */
public record CurriculumResponse(Long curriculumId, String curriculumTitle, int totalLessons, int completedLessons,
		CurriculumStatus status) {

	public static CurriculumResponse from(CurriculumResult result) {
		return new CurriculumResponse(result.id(), result.title(), result.totalLessons(), result.completedLessons(),
				result.status());
	}
}
