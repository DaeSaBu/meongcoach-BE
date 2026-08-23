package com.daesabu.meongcoach.ai.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VideoAnalysisFailedException")
class VideoAnalysisFailedExceptionTest {

	@Test
	@DisplayName("영상 분석에 실패하면 AI_VIDEO_ANALYSIS_FAILED 코드를 갖는다")
	void hasVideoAnalysisFailedErrorCode() {
		DomainException exception = new VideoAnalysisFailedException("응답이 JSON 형식이 아닙니다");

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_VIDEO_ANALYSIS_FAILED);
		assertThat(exception.getMessage()).contains("JSON");
	}

	@Test
	@DisplayName("원인 예외를 보존한다")
	void preservesCause() {
		IllegalStateException cause = new IllegalStateException("원인");

		DomainException exception = new VideoAnalysisFailedException("분석 실패", cause);

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
