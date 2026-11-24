package com.daesabu.meongcoach.report.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.application.required.ReportRepository;
import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.ReportReason;
import com.daesabu.meongcoach.report.domain.exception.InvalidReportReasonException;
import com.daesabu.meongcoach.report.domain.exception.ReportAlreadyRegisteredException;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class ReportRegisterTest {

	private static final Long REPORTER_ID = 7L;

	private static final Long OTHER_REPORTER_ID = 8L;

	private static final Long TARGET_ID = 30L;

	private static final ReportRegisterRequest REQUEST = new ReportRegisterRequest("SPAM", "같은 광고를 계속 올려요");

	@Autowired
	private ReportRegister reportRegister;

	@Autowired
	private ReportRepository reportRepository;

	@Test
	void 신고를_접수하면_신고자와_대상과_사유와_상세_내용을_저장한다() {
		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST);

		List<Report> reports = reportRepository.findAll();
		assertThat(reports).hasSize(1);
		Report report = reports.getFirst();
		assertThat(report.getId()).isNotNull();
		assertThat(report.getReporterId()).isEqualTo(REPORTER_ID);
		assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMUNITY_POST);
		assertThat(report.getTargetId()).isEqualTo(TARGET_ID);
		assertThat(report.getReason()).isEqualTo(ReportReason.SPAM);
		assertThat(report.getDetail()).isEqualTo("같은 광고를 계속 올려요");
		assertThat(report.getCreatedAt()).isNotNull();
	}

	@Test
	void 상세_내용_없이_신고할_수_있다() {
		reportRegister.register(
				REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, new ReportRegisterRequest("MISINFORMATION", null));

		Report report = reportRepository.findAll().getFirst();
		assertThat(report.getReason()).isEqualTo(ReportReason.MISINFORMATION);
		assertThat(report.getDetail()).isNull();
	}

	@Test
	void 같은_대상을_다시_신고하면_예외를_던지고_신고는_한_건만_남는다() {
		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST);

		assertThatThrownBy(() -> reportRegister.register(
				REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, new ReportRegisterRequest("ABUSE", null)))
				.isInstanceOf(ReportAlreadyRegisteredException.class);
		assertThat(reportRepository.findAll()).hasSize(1);
	}

	@Test
	void 다른_회원은_같은_대상을_신고할_수_있다() {
		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST);

		reportRegister.register(OTHER_REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST);

		assertThat(reportRepository.findAll()).hasSize(2);
	}

	@Test
	void 대상_ID가_같아도_대상_종류가_다르면_신고할_수_있다() {
		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST);

		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_COMMENT, TARGET_ID, REQUEST);

		assertThat(reportRepository.findAll()).hasSize(2);
	}

	@Test
	void 지원하지_않는_사유로는_신고할_수_없다() {
		ReportRegisterRequest request = new ReportRegisterRequest("BORING", null);

		assertThatThrownBy(
				() -> reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, request))
				.isInstanceOf(InvalidReportReasonException.class);
		assertThat(reportRepository.findAll()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void 사유가_비어_있으면_검증에_실패한다(String reason) {
		ReportRegisterRequest request = new ReportRegisterRequest(reason, null);

		assertThatThrownBy(
				() -> reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 상세_내용이_500자를_넘으면_검증에_실패한다() {
		ReportRegisterRequest request = new ReportRegisterRequest("OTHER", "가".repeat(501));

		assertThatThrownBy(
				() -> reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
