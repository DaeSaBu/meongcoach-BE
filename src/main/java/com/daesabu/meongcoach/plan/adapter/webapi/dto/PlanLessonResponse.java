package com.daesabu.meongcoach.plan.adapter.webapi.dto;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanLessonProgressResult;

public record PlanLessonResponse(Long lessonId, String title, boolean completed) {

	public static PlanLessonResponse from(PlanLessonProgressResult result) {
		return new PlanLessonResponse(result.lessonId(), result.title(), result.completed());
	}
}
