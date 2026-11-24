package com.daesabu.meongcoach.report.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.application.required.ReportRepository;
import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.ReportReason;
import com.daesabu.meongcoach.report.domain.ReportRegisterCommand;
import com.daesabu.meongcoach.report.domain.exception.ReportAlreadyRegisteredException;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class ReportRegisterConcurrencyTest {

	private static final Long REPORTER_ID = 753002L;

	private static final Long TARGET_ID = 30L;

	private static final ReportRegisterRequest REQUEST = new ReportRegisterRequest("SPAM", null);

	@Autowired
	private ReportRegister reportRegister;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> lockReleased = new CompletableFuture<>();

	@AfterEach
	void tearDown() {
		lockReleased.complete(null);
		executor.close();
		reportRepository.deleteAll();
	}

	@Test
	void 같은_회원이_같은_대상을_신고하는_중이면_끝날_때까지_기다린_뒤_이미_신고한_대상으로_거절한다() throws Exception {
		registerHoldingLock();

		Future<?> registration = executor.submit(
				() -> reportRegister.register(REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, REQUEST));

		assertThatThrownBy(() -> registration.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		assertThatThrownBy(() -> registration.get(5, SECONDS))
				.hasCauseInstanceOf(ReportAlreadyRegisteredException.class);
		assertThat(reportRepository.findAll()).hasSize(1);
	}

	private void registerHoldingLock() throws Exception {
		ReportRegisterCommand command = new ReportRegisterCommand(
				REPORTER_ID, ReportTargetType.COMMUNITY_POST, TARGET_ID, ReportReason.SPAM, null);
		CompletableFuture<Void> lockAcquired = new CompletableFuture<>();
		executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
			reportRepository.lockByReporterId(REPORTER_ID);
			reportRepository.saveAndFlush(Report.register(command));
			lockAcquired.complete(null);
			lockReleased.join();
		}));
		lockAcquired.get(5, SECONDS);
	}
}
