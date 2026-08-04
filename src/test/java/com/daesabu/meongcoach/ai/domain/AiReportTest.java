package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AiReport 도메인")
class AiReportTest {

	@Test
	@DisplayName("생성하면 사용자·영상 객체 키·리포트 내용이 설정된다")
	void createSetsUserVideoObjectKeyAndContent() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoObjectKey()).isEqualTo("videos/training/1/video.mp4");
		assertThat(report.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("무료 체험 최대 횟수는 3회다")
	void maxTrialCountIsThree() {
		assertThat(AiReport.MAX_TRIAL_COUNT).isEqualTo(3);
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1, 2})
	@DisplayName("생성된 리포트가 한도 미만이면 체험 검증을 통과한다")
	void validateTrialAvailablePassesWhenCountIsBelowMax(long generatedCount) {
		assertThatCode(() -> AiReport.validateTrialAvailable(generatedCount))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(longs = {3, 4})
	@DisplayName("생성된 리포트가 한도 이상이면 체험 소진 예외를 던진다")
	void validateTrialAvailableThrowsWhenCountReachesMax(long generatedCount) {
		assertThatThrownBy(() -> AiReport.validateTrialAvailable(generatedCount))
				.isInstanceOf(AiReportTrialExceededException.class);
	}
}
