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
	@DisplayName("모든 코드는 한국어 메시지를 갖는다")
	void allCodesHaveKoreanMessage(AiErrorCode errorCode) {
		assertThat(errorCode.message()).isNotBlank();
	}

	@Test
	@DisplayName("리포트 미존재는 404, 체험 횟수 소진은 403 상태를 갖는다")
	void definesStatusPerErrorCode() {
		assertThat(AiErrorCode.AI_REPORT_NOT_FOUND.status()).isEqualTo(404);
		assertThat(AiErrorCode.AI_REPORT_TRIAL_EXCEEDED.status()).isEqualTo(403);
	}

	@Test
	@DisplayName("리포트 미존재와 체험 횟수 소진 두 가지 코드를 정의한다")
	void definesTwoErrorCodes() {
		assertThat(AiErrorCode.values()).containsExactly(AiErrorCode.AI_REPORT_NOT_FOUND,
				AiErrorCode.AI_REPORT_TRIAL_EXCEEDED);
	}
}
