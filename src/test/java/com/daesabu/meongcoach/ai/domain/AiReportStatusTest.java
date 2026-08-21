package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AiReportStatus")
class AiReportStatusTest {

	@ParameterizedTest
	@EnumSource(value = AiReportStatus.class, names = "FAILED_.*", mode = EnumSource.Mode.MATCH_ANY)
	@DisplayName("FAILED_ 상태는 실패 결말이다")
	void failedStatusesAreFailures(AiReportStatus status) {
		assertThat(status.isFailure()).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = AiReportStatus.class, names = {"PENDING", "COMPLETED"})
	@DisplayName("진행 중·완료 상태는 실패 결말이 아니다")
	void pendingAndCompletedAreNotFailures(AiReportStatus status) {
		assertThat(status.isFailure()).isFalse();
	}
}
