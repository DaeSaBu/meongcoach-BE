package com.daesabu.meongcoach.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
	void 메시지는_기본적으로_에러_코드의_메시지를_사용한다() {
		DomainException exception = new TestNotFoundException();

		assertThat(exception.getMessage()).isEqualTo("테스트 리소스를 찾을 수 없습니다.");
		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.TEST_NOT_FOUND);
	}

	@Test
	void 상세_메시지를_전달하면_메시지를_덮어쓸_수_있다() {
		DomainException exception = new TestNotFoundException("id가 1인 테스트 리소스를 찾을 수 없습니다.");

		assertThat(exception.getMessage()).isEqualTo("id가 1인 테스트 리소스를 찾을 수 없습니다.");
		assertThat(exception.getErrorCode()).isEqualTo(TestErrorCode.TEST_NOT_FOUND);
	}
}
