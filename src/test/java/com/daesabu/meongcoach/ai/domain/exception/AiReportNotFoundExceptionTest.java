package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReportNotFoundException")
class AiReportNotFoundExceptionTest {

	@Test
	@DisplayName("리포트를 찾지 못하면 AI_REPORT_NOT_FOUND 코드를 갖는다")
	void hasAiReportNotFoundErrorCode() {
		DomainException exception = new AiReportNotFoundException(2L);

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_NOT_FOUND);
		assertThat(exception.getMessage()).contains("2");
	}
}
