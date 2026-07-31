package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TopicNotConfiguredException")
class TopicNotConfiguredExceptionTest {

	@Test
	@DisplayName("등록된 토픽이 하나도 없으면 TRAINING_TOPIC_NOT_CONFIGURED 코드를 갖는다")
	void hasTopicNotConfiguredErrorCode() {
		DomainException exception = new TopicNotConfiguredException();

		assertThat(exception.getErrorCode()).isEqualTo(TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED);
		assertThat(exception.getMessage()).isEqualTo(TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED.message());
	}
}
