package com.daesabu.meongcoach.analysis.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class AiVideoDurationOutOfRangeExceptionTest {

	@Test
	void 영상_길이가_범위를_벗어나면_AI_VIDEO_DURATION_OUT_OF_RANGE_코드를_갖는다() {
		DomainException exception = new AiVideoDurationOutOfRangeException();

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_VIDEO_DURATION_OUT_OF_RANGE);
		assertThat(exception.getMessage()).isNotBlank();
	}
}
