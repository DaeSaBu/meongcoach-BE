package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * AI 리포트 무료 체험 조회 서비스 검증.
 * 한도·잔여 횟수 계산 자체는 AiTrialTest가 다루므로 여기서는 조회 책임만 검증한다.
 */
@DataJpaTest
@Import(AiReportTrialFinderService.class)
class AiReportTrialFinderServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Autowired
	private AiReportRepository aiReportRepository;

	@Test
	void 리포트가_없으면_사용_0회로_조회된다() {
		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	@Test
	void 완료한_리포트_수가_그대로_사용_횟수가_된다() {
		persistCompletedReport(USER_ID, "videos/training/42/first.mp4");
		persistCompletedReport(USER_ID, "videos/training/42/second.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(2));
	}

	@Test
	void 실패한_리포트는_사용_횟수에_세지_않는다() {
		persistCompletedReport(USER_ID, "videos/training/42/first.mp4");
		persistFailedReport(USER_ID, "videos/training/42/failed.mp4", AiReport::failByAnalysis);
		persistFailedReport(USER_ID, "videos/training/42/exceeded.mp4", AiReport::failByTrialExceeded);

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(1));
	}

	@Test
	void 업로드_대기_중이거나_분석_중인_리포트는_사용_횟수에_세지_않는다() {
		aiReportRepository.saveAndFlush(uploadingReport(USER_ID, "videos/training/42/uploading.mp4"));
		aiReportRepository.saveAndFlush(pendingReport(USER_ID, "videos/training/42/pending.mp4"));

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	@Test
	void 다른_사용자의_리포트는_사용_횟수에_세지_않는다() {
		persistCompletedReport(OTHER_USER_ID, "videos/training/99/other.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	private void persistCompletedReport(Long userId, String videoObjectKey) {
		AiReport report = pendingReport(userId, videoObjectKey);
		report.complete("분리불안 징후 행동 분석", "분리불안 징후가 관찰됩니다.");
		aiReportRepository.saveAndFlush(report);
	}

	private void persistFailedReport(Long userId, String videoObjectKey, Consumer<AiReport> failure) {
		AiReport report = pendingReport(userId, videoObjectKey);
		failure.accept(report);
		aiReportRepository.saveAndFlush(report);
	}

	private static AiReport uploadingReport(Long userId, String videoObjectKey) {
		return AiReport.uploading(new AiReportUploadCommand(userId, videoObjectKey, LocalDateTime.now().plusMinutes(15)));
	}

	private static AiReport pendingReport(Long userId, String videoObjectKey) {
		AiReport report = uploadingReport(userId, videoObjectKey);
		report.startAnalysis();
		return report;
	}
}
