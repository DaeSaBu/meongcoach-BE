package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AiReportTrialFinderService.class)
class AiReportTrialFinderServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@Test
	void 리포트가_없으면_사용_0회로_조회된다() {
		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	@Test
	void 완료한_리포트_수가_그대로_사용_횟수가_된다() {
		persistCompletedAnalysis(USER_ID, "videos/training/42/first.mp4");
		persistCompletedAnalysis(USER_ID, "videos/training/42/second.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(2));
	}

	@Test
	void 실패한_리포트는_사용_횟수에_세지_않는다() {
		persistCompletedAnalysis(USER_ID, "videos/training/42/first.mp4");
		persistFailedAnalysis(USER_ID, "videos/training/42/failed.mp4", AiAnalysis::failByAnalysis);
		persistFailedAnalysis(USER_ID, "videos/training/42/exceeded.mp4", AiAnalysis::failByTrialExceeded);

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(1));
	}

	@Test
	void 업로드_대기_중이거나_분석_중인_리포트는_사용_횟수에_세지_않는다() {
		aiAnalysisRepository.saveAndFlush(uploadingAnalysis(USER_ID, "videos/training/42/uploading.mp4"));
		aiAnalysisRepository.saveAndFlush(pendingAnalysis(USER_ID, "videos/training/42/pending.mp4"));

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	@Test
	void 다른_사용자의_리포트는_사용_횟수에_세지_않는다() {
		persistCompletedAnalysis(OTHER_USER_ID, "videos/training/99/other.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	private void persistCompletedAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = pendingAnalysis(userId, videoObjectKey);
		analysis.complete("분리불안 징후 행동 분석", "분리불안 징후가 관찰됩니다.");
		aiAnalysisRepository.saveAndFlush(analysis);
	}

	private void persistFailedAnalysis(Long userId, String videoObjectKey, Consumer<AiAnalysis> failure) {
		AiAnalysis analysis = pendingAnalysis(userId, videoObjectKey);
		failure.accept(analysis);
		aiAnalysisRepository.saveAndFlush(analysis);
	}

	private static AiAnalysis uploadingAnalysis(Long userId, String videoObjectKey) {
		return AiAnalysis.uploading(new AiAnalysisUploadCommand(userId, videoObjectKey, LocalDateTime.now().plusMinutes(15)));
	}

	private static AiAnalysis pendingAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = uploadingAnalysis(userId, videoObjectKey);
		analysis.startAnalysis();
		return analysis;
	}
}
