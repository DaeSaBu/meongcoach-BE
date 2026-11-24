package com.daesabu.meongcoach.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import org.junit.jupiter.api.Test;

class ReportTest {

	private static final Long REPORTER_ID = 7L;

	private static final Long TARGET_ID = 30L;

	@Test
	void 신고를_등록하면_신고자와_대상과_사유와_상세_내용을_담는다() {
		ReportRegisterCommand command = new ReportRegisterCommand(
				REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, ReportReason.SPAM, "같은 광고를 계속 올려요");

		Report report = Report.register(command);

		assertThat(report.getReporterId()).isEqualTo(REPORTER_ID);
		assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMUNITY_POST);
		assertThat(report.getTargetId()).isEqualTo(TARGET_ID);
		assertThat(report.getReason()).isEqualTo(ReportReason.SPAM);
		assertThat(report.getDetail()).isEqualTo("같은 광고를 계속 올려요");
	}

	@Test
	void 상세_내용_없이_등록할_수_있다() {
		ReportRegisterCommand command = new ReportRegisterCommand(
				REPORTER_ID, ReportTargetType.COMMUNITY_COMMENT, TARGET_ID, ReportReason.ABUSE, null);

		Report report = Report.register(command);

		assertThat(report.getDetail()).isNull();
	}
}
