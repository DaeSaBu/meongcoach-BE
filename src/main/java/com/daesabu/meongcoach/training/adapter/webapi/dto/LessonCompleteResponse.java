package com.daesabu.meongcoach.training.adapter.webapi.dto;

/**
 * 레슨 완료 응답. 완료한 레슨 ID와 증가가 반영된 반복 완료 횟수를 담는다.
 */
public record LessonCompleteResponse(Long lessonId, int completedCount) {

	public static LessonCompleteResponse from(Long lessonId, int completedCount) {
		return new LessonCompleteResponse(lessonId, completedCount);
	}
}
