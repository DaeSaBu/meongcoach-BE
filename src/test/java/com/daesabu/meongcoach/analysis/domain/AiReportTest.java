package com.daesabu.meongcoach.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiReportTest {

	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";

	@Test
	void 제목과_본문으로_생성한다() {
		AiReport report = AiReport.of(TITLE, CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
		assertThat(report.getContent()).isEqualTo(CONTENT);
	}

	@Test
	void 제목_없이도_생성할_수_있다() {
		AiReport report = AiReport.of(null, CONTENT);

		assertThat(report.getTitle()).isNull();
	}

	@Test
	void 본문_없이는_생성할_수_없다() {
		assertThatThrownBy(() -> AiReport.of(TITLE, null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void 컬럼_길이를_넘는_제목은_잘라서_저장한다() {
		AiReport report = AiReport.of("가".repeat(500), CONTENT);

		assertThat(report.getTitle()).hasSize(AiReport.TITLE_MAX_LENGTH);
	}

	@Test
	void 공백뿐인_제목은_null로_저장한다() {
		AiReport report = AiReport.of("   ", CONTENT);

		assertThat(report.getTitle()).isNull();
	}

	@Test
	void 제목_앞뒤_공백은_제거해_저장한다() {
		AiReport report = AiReport.of("  " + TITLE + "  ", CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
	}
}
