package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Lesson;

public record LessonResponse(Long lessonId, String lessonTitle, int lessonSortOrder, int estimatedMinutes,
		UserLessonProgressResponse userLessonProgress) {

	public static LessonResponse of(Lesson lesson, int completedCount) {
		return new LessonResponse(lesson.getId(), lesson.getTitle(), lesson.getSortOrder(), lesson.getEstimatedMinutes(),
				new UserLessonProgressResponse(completedCount));
	}
}
