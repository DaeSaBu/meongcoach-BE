package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CurriculumStatusTest {

	@Test
	void 레슨이_하나도_없으면_시작_전이다() {
		CurriculumStatus status = CurriculumStatus.of(0, 0);

		assertThat(status).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 완료한_레슨이_없으면_시작_전이다() {
		CurriculumStatus status = CurriculumStatus.of(5, 0);

		assertThat(status).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 일부_레슨만_완료하면_진행_중이다() {
		CurriculumStatus status = CurriculumStatus.of(5, 2);

		assertThat(status).isEqualTo(CurriculumStatus.IN_PROGRESS);
	}

	@Test
	void 모든_레슨을_완료하면_완료다() {
		CurriculumStatus status = CurriculumStatus.of(5, 5);

		assertThat(status).isEqualTo(CurriculumStatus.COMPLETED);
	}
}
