package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CurriculumNotFoundException")
class CurriculumNotFoundExceptionTest {

	@Test
	@DisplayName("커리큘럼을 찾지 못하면 TRAINING_CURRICULUM_NOT_FOUND 코드를 갖는다")
	void hasCurriculumNotFoundErrorCode() {
		DomainException exception = new CurriculumNotFoundException(2L);

		assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.TRAINING_CURRICULUM_NOT_FOUND);
		assertThat(exception.getMessage()).contains("2");
	}
}
