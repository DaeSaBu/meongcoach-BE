package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class AiReportTrialExceededExceptionTest {

	@Test
	void 체험_횟수를_소진하면_AI_REPORT_TRIAL_EXCEEDED_코드를_갖는다() {
		DomainException exception = new AiReportTrialExceededException();

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_REPORT_TRIAL_EXCEEDED);
		assertThat(exception.getMessage()).isNotBlank();
	}
}
