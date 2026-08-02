package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AiErrorCode")
class AiErrorCodeTest {

	@ParameterizedTest
	@EnumSource(AiErrorCode.class)
	@DisplayName("코드는 enum 상수 이름을 그대로 반환한다")
	void codeReturnsEnumConstantName(AiErrorCode errorCode) {
		assertThat(errorCode.code()).isEqualTo(errorCode.name());
	}

	@ParameterizedTest
	@EnumSource(AiErrorCode.class)
	@DisplayName("AI 리포트 조회 실패는 모두 404 상태와 한국어 메시지를 갖는다")
	void allCodesAreNotFoundWithKoreanMessage(AiErrorCode errorCode) {
		assertThat(errorCode.status()).isEqualTo(404);
		assertThat(errorCode.message()).isNotBlank();
	}

	@Test
	@DisplayName("리포트 미존재 한 가지 코드를 정의한다")
	void definesOneErrorCode() {
		assertThat(AiErrorCode.values()).containsExactly(AiErrorCode.AI_REPORT_NOT_FOUND);
	}
}
