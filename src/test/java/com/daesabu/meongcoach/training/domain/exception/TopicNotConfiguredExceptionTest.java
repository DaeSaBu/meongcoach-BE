package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class TopicNotConfiguredExceptionTest {

	@Test
	void 등록된_토픽이_하나도_없으면_TRAINING_TOPIC_NOT_CONFIGURED_코드를_갖는다() {
		DomainException exception = new TopicNotConfiguredException();

		assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED);
		assertThat(exception.getMessage()).isEqualTo(TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED.message());
	}
}
