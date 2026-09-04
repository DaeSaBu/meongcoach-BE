package com.daesabu.meongcoach.ai.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AiTrialTest {

	@Test
	void 무료_체험_최대_횟수는_3회다() {
		// 한도가 조용히 바뀌면 API 문서와 클라이언트 안내 문구가 어긋나므로 값 자체를 고정한다
		assertThat(AiTrial.MAX_COUNT).isEqualTo(3);
	}

	@Test
	void 리포지토리_count_결과로_생성한다() {
		assertThat(AiTrial.of(2L)).isEqualTo(new AiTrial(2));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	void 생성한_리포트가_한도_미만이면_체험이_남아_있다(int usedCount) {
		assertThat(new AiTrial(usedCount).isAvailable()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 4})
	void 생성한_리포트가_한도_이상이면_체험을_소진했다(int usedCount) {
		assertThat(new AiTrial(usedCount).isAvailable()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"0, 3", "1, 2", "2, 1", "3, 0"})
	void 사용한_만큼_잔여_횟수가_줄어든다(int usedCount, int expected) {
		assertThat(new AiTrial(usedCount).remainingCount()).isEqualTo(expected);
	}

	@Test
	void 한도를_넘겨_저장된_경우에도_잔여_횟수는_0으로_내려간다() {
		// 업로드 URL 발급과 리포트 생성이 비동기라 한도를 넘겨 저장될 수 있다
		assertThat(new AiTrial(4).remainingCount()).isZero();
	}

	@Test
	void 같은_사용_횟수끼리는_동등하다() {
		assertThat(new AiTrial(2)).isEqualTo(new AiTrial(2));
	}
}
