package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.application.required.AiTrialRepository;
import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiTrialFixture;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AiAnalysisModifyService.class)
class AiAnalysisModifyServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final VideoAnalysisResult RESULT = new VideoAnalysisResult("분리불안 징후 행동 분석", "분리불안 징후가 관찰됩니다.");

	@Autowired
	private AiAnalysisModifyService aiAnalysisModifyService;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 완료하면_분석_건을_리포트와_함께_COMPLETED로_저장한다() {
		AiAnalysis analysis = persistStartedAnalysis(USER_ID, "videos/training/42/first.mp4");

		aiAnalysisModifyService.complete(analysis, RESULT);
		entityManager.flush();
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(analysis.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
		assertThat(found.getTitle()).isEqualTo(RESULT.title());
		assertThat(found.getContent()).isEqualTo(RESULT.content());
	}

	@Test
	void 저장된_현황이_없는_사용자가_처음_완료하면_사용_1회인_현황을_만든다() {
		AiAnalysis analysis = persistStartedAnalysis(USER_ID, "videos/training/42/first.mp4");

		aiAnalysisModifyService.complete(analysis, RESULT);
		entityManager.flush();
		entityManager.clear();

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
		assertThat(trial.getUsedCount()).isEqualTo(1);
	}

	@Test
	void 완료할_때마다_저장된_사용_횟수가_1씩_늘고_부여_횟수는_그대로다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.create(USER_ID, 35, 12));
		AiAnalysis first = persistStartedAnalysis(USER_ID, "videos/training/42/first.mp4");
		AiAnalysis second = persistStartedAnalysis(USER_ID, "videos/training/42/second.mp4");

		aiAnalysisModifyService.complete(first, RESULT);
		aiAnalysisModifyService.complete(second, RESULT);
		entityManager.flush();
		entityManager.clear();

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.maxCount()).isEqualTo(35);
		assertThat(trial.getUsedCount()).isEqualTo(14);
	}

	@Test
	void 다른_사용자의_사용_횟수는_바꾸지_않는다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(OTHER_USER_ID, 3));
		AiAnalysis analysis = persistStartedAnalysis(USER_ID, "videos/training/42/first.mp4");

		aiAnalysisModifyService.complete(analysis, RESULT);
		entityManager.flush();
		entityManager.clear();

		assertThat(aiTrialRepository.findByUserId(OTHER_USER_ID).orElseThrow().getUsedCount()).isEqualTo(3);
	}

	private AiAnalysis persistStartedAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = AiAnalysis.uploading(
				new AiAnalysisUploadCommand(userId, videoObjectKey, Instant.now().plusSeconds(900), null));
		analysis.startAnalysis();
		return aiAnalysisRepository.saveAndFlush(analysis);
	}
}
