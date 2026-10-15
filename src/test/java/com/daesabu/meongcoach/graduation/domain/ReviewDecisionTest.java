package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidReviewDecisionException;
import org.junit.jupiter.api.Test;

class ReviewDecisionTest {

	@Test
	void 판정_문자열을_판정으로_바꾼다() {
		assertThat(ReviewDecision.from("APPROVED")).isEqualTo(ReviewDecision.APPROVED);
		assertThat(ReviewDecision.from("REVISION_REQUESTED")).isEqualTo(ReviewDecision.REVISION_REQUESTED);
	}

	@Test
	void 지원하지_않는_판정이면_예외를_던진다() {
		assertThatThrownBy(() -> ReviewDecision.from("REJECTED")).isInstanceOf(InvalidReviewDecisionException.class);
		assertThatThrownBy(() -> ReviewDecision.from(null)).isInstanceOf(InvalidReviewDecisionException.class);
	}
}
