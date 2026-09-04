package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class CurriculumNotFoundExceptionTest {

	@Test
	void 커리큘럼을_찾지_못하면_TRAINING_CURRICULUM_NOT_FOUND_코드를_갖는다() {
		DomainException exception = new CurriculumNotFoundException(2L);

		assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.TRAINING_CURRICULUM_NOT_FOUND);
		assertThat(exception.getMessage()).contains("2");
	}
}
