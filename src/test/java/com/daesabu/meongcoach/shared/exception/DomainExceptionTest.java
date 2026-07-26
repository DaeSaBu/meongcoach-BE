package com.daesabu.meongcoach.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainException")
class DomainExceptionTest {

	private enum TestErrorCode implements ErrorCode {
		TEST_NOT_FOUND(404, "테스트 리소스를 찾을 수 없습니다.");

		private final int status;
		private final String message;

		TestErrorCode(int status, String message) {
			this.status = status;
			this.message = message;
		}

		@Override
		public String code() {
			return name();
		}

		@Override
		public String message() {
			return message;
		}

		@Override
		public int status() {
			return status;
		}
	}

	private static class TestNotFoundException extends DomainException {

		TestNotFoundException() {
			super(TestErrorCode.TEST_NOT_FOUND);
		}

		TestNotFoundException(String detail) {
			super(TestErrorCode.TEST_NOT_FOUND, detail);
		}
	}

	@Test
	@DisplayName("메시지는 기본적으로 에러 코드의 메시지를 사용한다")
	void messageDefaultsToErrorCodeMessage() {
		DomainException exception = new TestNotFoundException();

		assertThat(exception.getMessage()).isEqualTo("테스트 리소스를 찾을 수 없습니다.");
		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.TEST_NOT_FOUND);
	}

	@Test
	@DisplayName("상세 메시지를 전달하면 메시지를 덮어쓸 수 있다")
	void messageCanBeOverriddenWithDetail() {
		DomainException exception = new TestNotFoundException("id가 1인 테스트 리소스를 찾을 수 없습니다.");

		assertThat(exception.getMessage()).isEqualTo("id가 1인 테스트 리소스를 찾을 수 없습니다.");
		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.TEST_NOT_FOUND);
	}
}
