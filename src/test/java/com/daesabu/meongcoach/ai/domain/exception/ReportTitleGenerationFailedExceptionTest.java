package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class ReportTitleGenerationFailedExceptionTest {

	@Test
	void 제목_생성에_실패하면_AI_REPORT_TITLE_GENERATION_FAILED_코드를_갖는다() {
		DomainException exception = new ReportTitleGenerationFailedException("제목이 비어 있습니다");

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_TITLE_GENERATION_FAILED);
		assertThat(exception.getMessage()).contains("비어");
	}

	@Test
	void 원인_예외를_보존한다() {
		IllegalStateException cause = new IllegalStateException("원인");

		DomainException exception = new ReportTitleGenerationFailedException("제목 생성 실패", cause);

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
