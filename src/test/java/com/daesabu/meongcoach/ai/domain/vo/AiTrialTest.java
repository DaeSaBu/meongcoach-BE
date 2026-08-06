package com.daesabu.meongcoach.ai.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AiTrial 값 객체")
class AiTrialTest {

	@Test
	@DisplayName("무료 체험 최대 횟수는 3회다")
	void maxCountIsThree() {
		// 한도가 조용히 바뀌면 API 문서와 클라이언트 안내 문구가 어긋나므로 값 자체를 고정한다
		assertThat(AiTrial.MAX_COUNT).isEqualTo(3);
	}

	@Test
	@DisplayName("리포지토리 count 결과로 생성한다")
	void ofNarrowsRepositoryCount() {
		assertThat(AiTrial.of(2L)).isEqualTo(new AiTrial(2));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	@DisplayName("생성한 리포트가 한도 미만이면 체험이 남아 있다")
	void isAvailableWhenUsedCountIsBelowMax(int usedCount) {
		assertThat(new AiTrial(usedCount).isAvailable()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 4})
	@DisplayName("생성한 리포트가 한도 이상이면 체험을 소진했다")
	void isNotAvailableWhenUsedCountReachesMax(int usedCount) {
		assertThat(new AiTrial(usedCount).isAvailable()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"0, 3", "1, 2", "2, 1", "3, 0"})
	@DisplayName("사용한 만큼 잔여 횟수가 줄어든다")
	void remainingCountDecreasesAsUsed(int usedCount, int expected) {
		assertThat(new AiTrial(usedCount).remainingCount()).isEqualTo(expected);
	}

	@Test
	@DisplayName("한도를 넘겨 저장된 경우에도 잔여 횟수는 0으로 내려간다")
	void remainingCountIsClampedToZero() {
		// 업로드 URL 발급과 리포트 생성이 비동기라 한도를 넘겨 저장될 수 있다
		assertThat(new AiTrial(4).remainingCount()).isZero();
	}

	@Test
	@DisplayName("같은 사용 횟수끼리는 동등하다")
	void sameUsedCountsAreEqual() {
		assertThat(new AiTrial(2)).isEqualTo(new AiTrial(2));
	}
}
