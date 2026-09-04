package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class VideoAnalysisFailedExceptionTest {

	@Test
	void 영상_분석에_실패하면_AI_VIDEO_ANALYSIS_FAILED_코드를_갖는다() {
		DomainException exception = new VideoAnalysisFailedException("응답이 JSON 형식이 아닙니다");

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_VIDEO_ANALYSIS_FAILED);
		assertThat(exception.getMessage()).contains("JSON");
	}

	@Test
	void 원인_예외를_보존한다() {
		IllegalStateException cause = new IllegalStateException("원인");

		DomainException exception = new VideoAnalysisFailedException("분석 실패", cause);

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
