package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Lesson;

public record LessonSummaryResult(Long id, String title) {

	public static LessonSummaryResult from(Lesson lesson) {
		return new LessonSummaryResult(lesson.getId(), lesson.getTitle());
	}
}
