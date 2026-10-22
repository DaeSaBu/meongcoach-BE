package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class LessonCompletionConflictException extends DomainException {

	public LessonCompletionConflictException(Long lessonId, Throwable cause) {
		super(TrainingErrorCode.TRAINING_LESSON_COMPLETION_CONFLICT,
				"id가 " + lessonId + "인 레슨의 완료가 동시에 처리되었습니다.", cause);
	}
}
