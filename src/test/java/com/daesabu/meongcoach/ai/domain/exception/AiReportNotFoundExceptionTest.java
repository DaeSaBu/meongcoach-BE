package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class AiReportNotFoundExceptionTest {

	@Test
	void 리포트를_찾지_못하면_AI_REPORT_NOT_FOUND_코드를_갖는다() {
		DomainException exception = new AiReportNotFoundException(2L);

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_NOT_FOUND);
		assertThat(exception.getMessage()).contains("2");
	}
}
