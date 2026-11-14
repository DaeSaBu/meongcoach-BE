package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumProgressResult;
import com.daesabu.meongcoach.training.domain.ProgressStatus;
import java.util.List;

public record PlanCurriculumResponse(Long curriculumId, Long topicId, String topicTitle, String title,
		String description, int totalLessons, int completedLessons, ProgressStatus status, boolean locked,
		List<PlanLessonResponse> lessons) {

	public static PlanCurriculumResponse from(PlanCurriculumProgressResult result) {
		List<PlanLessonResponse> planLessons = result.lessons().stream().map(PlanLessonResponse::from).toList();
		return new PlanCurriculumResponse(result.curriculumId(), result.topicId(), result.topicTitle(), result.title(),
				result.description(), result.totalLessons(), result.completedLessons(), result.status(),
				result.locked(), planLessons);
	}
}
