package com.daesabu.meongcoach.report.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.application.required.ReportRepository;
import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.exception.InvalidReportReasonException;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class ReportRegisterAlertTest {

	private static final Long REPORTER_ID = 753003L;

	private static final Long TARGET_ID = 30L;

	private static final long ASYNC_TIMEOUT_MILLIS = 2000;

	private static final long ASYNC_SETTLE_MILLIS = 300;

	private static final ReportRegisterRequest REQUEST = new ReportRegisterRequest("ANIMAL_CRUELTY", "강아지를 때려요");

	@Autowired
	private ReportRegister reportRegister;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private AlertSender alertSender;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void tearDown() {
		reportRepository.deleteAll();
	}

	@Test
	void 신고가_커밋되면_신고_정보를_알린다() {
		reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_COMMENT, TARGET_ID, REQUEST);

		Report report = reportRepository.findAll().getFirst();
		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(captor.capture());
		Alert alert = captor.getValue();
		assertThat(alert.title()).isEqualTo("신고 접수");
		assertThat(alert.fields()).containsExactly(
				AlertField.of("reportId", report.getId()),
				AlertField.of("targetType", "COMMUNITY_COMMENT"),
				AlertField.of("targetId", TARGET_ID),
				AlertField.of("reason", "ANIMAL_CRUELTY"),
				AlertField.of("reporterId", REPORTER_ID),
				AlertField.of("detail", "강아지를 때려요"));
	}

	@Test
	void 신고_트랜잭션이_롤백되면_알리지_않는다() {
		transactionTemplate.executeWithoutResult(status -> {
			reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_COMMENT, TARGET_ID, REQUEST);
			status.setRollbackOnly();
		});

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 신고가_거절되면_알리지_않는다() {
		ReportRegisterRequest invalidReason = new ReportRegisterRequest("BORING", null);

		assertThatThrownBy(() -> reportRegister.register(
				REPORTER_ID, ReportTargetType.COMMUNITY_COMMENT, TARGET_ID, invalidReason))
				.isInstanceOf(InvalidReportReasonException.class);

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}
}
