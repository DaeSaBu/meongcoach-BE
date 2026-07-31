package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.LessonView;

/**
 * 레슨 응답. 사용자의 진행도를 중첩 객체로 담는다.
 */
public record LessonResponse(Long lessonId, String lessonTitle, int lessonSortOrder, int estimatedMinutes,
		UserLessonProgressResponse userLessonProgress) {

	public static LessonResponse from(LessonView view) {
		return new LessonResponse(view.id(), view.title(), view.sortOrder(), view.estimatedMinutes(),
				new UserLessonProgressResponse(view.completedCount()));
	}
}
