package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LessonNotFoundException")
class LessonNotFoundExceptionTest {

	@Test
	@DisplayName("레슨을 찾지 못하면 TRAINING_LESSON_NOT_FOUND 코드를 갖는다")
	void hasLessonNotFoundErrorCode() {
		DomainException exception = new LessonNotFoundException(3L);

		assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.TRAINING_LESSON_NOT_FOUND);
		assertThat(exception.getMessage()).contains("3");
	}
}
