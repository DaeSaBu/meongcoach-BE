package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class LessonNotFoundException extends DomainException {

	public LessonNotFoundException(Long lessonId) {
		super(TrainingErrorCode.TRAINING_LESSON_NOT_FOUND, "id가 " + lessonId + "인 레슨을 찾을 수 없습니다.");
	}
}
