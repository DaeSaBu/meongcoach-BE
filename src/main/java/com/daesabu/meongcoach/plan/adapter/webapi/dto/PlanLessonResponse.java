package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.PlanLessonResult;

public record PlanLessonResponse(Long lessonId, String title, boolean completed) {

	public static PlanLessonResponse from(PlanLessonResult result) {
		return new PlanLessonResponse(result.id(), result.title(), result.completed());
	}
}
