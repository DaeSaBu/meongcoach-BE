package com.daesabu.meongcoach.training.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("TrainingErrorCode")
class TrainingErrorCodeTest {

	@ParameterizedTest
	@EnumSource(TrainingErrorCode.class)
	@DisplayName("코드는 enum 상수 이름을 그대로 반환한다")
	void codeReturnsEnumConstantName(TrainingErrorCode errorCode) {
		assertThat(errorCode.code()).isEqualTo(errorCode.name());
	}

	@ParameterizedTest
	@EnumSource(TrainingErrorCode.class)
	@DisplayName("훈련 콘텐츠 조회 실패는 모두 404 상태와 한국어 메시지를 갖는다")
	void allCodesAreNotFoundWithKoreanMessage(TrainingErrorCode errorCode) {
		assertThat(errorCode.status()).isEqualTo(404);
		assertThat(errorCode.message()).isNotBlank();
	}

	@Test
	@DisplayName("토픽·커리큘럼·레슨·토픽 미설정 네 가지 코드를 정의한다")
	void definesFourErrorCodes() {
		assertThat(TrainingErrorCode.values()).containsExactly(
				TrainingErrorCode.TRAINING_TOPIC_NOT_FOUND,
				TrainingErrorCode.TRAINING_CURRICULUM_NOT_FOUND,
				TrainingErrorCode.TRAINING_LESSON_NOT_FOUND,
				TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED);
	}
}
