package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReportTrialExceededException")
class AiReportTrialExceededExceptionTest {

	@Test
	@DisplayName("체험 횟수를 소진하면 AI_REPORT_TRIAL_EXCEEDED 코드를 갖는다")
	void hasAiReportTrialExceededErrorCode() {
		DomainException exception = new AiReportTrialExceededException();

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_TRIAL_EXCEEDED);
		assertThat(exception.getMessage()).isNotBlank();
	}
}
