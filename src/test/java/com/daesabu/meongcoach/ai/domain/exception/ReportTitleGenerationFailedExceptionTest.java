package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReportTitleGenerationFailedException")
class ReportTitleGenerationFailedExceptionTest {

	@Test
	@DisplayName("제목 생성에 실패하면 AI_REPORT_TITLE_GENERATION_FAILED 코드를 갖는다")
	void hasReportTitleGenerationFailedErrorCode() {
		DomainException exception = new ReportTitleGenerationFailedException("제목이 비어 있습니다");

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_TITLE_GENERATION_FAILED);
		assertThat(exception.getMessage()).contains("비어");
	}

	@Test
	@DisplayName("원인 예외를 보존한다")
	void preservesCause() {
		IllegalStateException cause = new IllegalStateException("원인");

		DomainException exception = new ReportTitleGenerationFailedException("제목 생성 실패", cause);

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
