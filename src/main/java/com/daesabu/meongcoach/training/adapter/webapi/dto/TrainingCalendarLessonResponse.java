package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarLessonResult;
import com.daesabu.meongcoach.training.domain.Lesson;

public record TrainingCalendarLessonResponse(Long lessonId, String lessonTitle, int completedCount) {

	public static TrainingCalendarLessonResponse from(TrainingCalendarLessonResult result) {
		Lesson lesson = result.lesson();
		return new TrainingCalendarLessonResponse(lesson.getId(), lesson.getTitle(), result.trainedDays());
	}
}
