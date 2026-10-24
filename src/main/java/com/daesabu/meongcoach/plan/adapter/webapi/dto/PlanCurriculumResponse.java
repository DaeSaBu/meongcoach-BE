package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.util.List;

public record PlanCurriculumResponse(Long curriculumId, String title, String description, int totalLessons,
		int completedLessons, ProgressStatus status, boolean locked, List<PlanLessonResponse> lessons) {

	public static PlanCurriculumResponse from(PlanCurriculumResult result) {
		List<PlanLessonResponse> planLessons = result.lessons().stream().map(PlanLessonResponse::from).toList();
		return new PlanCurriculumResponse(result.id(), result.title(), result.description(), result.totalLessons(),
				result.completedLessons(), result.status(), result.locked(), planLessons);
	}
}
