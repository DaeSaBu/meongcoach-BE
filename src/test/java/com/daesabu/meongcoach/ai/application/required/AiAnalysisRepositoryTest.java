package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class AiAnalysisRepositoryTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/7/key.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";
	private static final Instant UPLOAD_EXPIRES_AT = Instant.parse("2026-08-21T12:15:00Z");

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 완료한_분석을_리포트와_함께_저장하고_다시_조회할_수_있다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(completedAnalysis(7L, VIDEO_OBJECT_KEY, TITLE, CONTENT));
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getUserId()).isEqualTo(7L);
		assertThat(found.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(found.getTitle()).isEqualTo(TITLE);
		assertThat(found.getContent()).isEqualTo(CONTENT);
		assertThat(found.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 제목이_없는_리포트도_저장하고_다시_조회할_수_있다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(completedAnalysis(7L, VIDEO_OBJECT_KEY, null, CONTENT));
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getTitle()).isNull();
		assertThat(found.getContent()).isEqualTo(CONTENT);
	}

	@Test
	void 결과가_없는_UPLOADING_분석도_업로드_만료_시각과_함께_저장하고_다시_조회할_수_있다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(uploadingAnalysis(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
		assertThat(found.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(found.getReport()).isNull();
	}

	@Test
	void 준영속_분석의_상태를_전이한_뒤_다시_저장하면_갱신된_상태로_조회된다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(pendingAnalysis(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		saved.failByAnalysis();
		aiAnalysisRepository.saveAndFlush(saved);
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(AiAnalysisStatus.FAILED_ANALYSIS);
	}

	@Test
	void 준영속_분석을_완료한_뒤_다시_저장하면_리포트까지_저장되고_연결된다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(pendingAnalysis(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		saved.complete(TITLE, CONTENT);
		aiAnalysisRepository.saveAndFlush(saved);
		entityManager.clear();

		AiAnalysis found = aiAnalysisRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
		assertThat(found.getReport().getId()).isNotNull();
		assertThat(found.getReport().getCreatedAt()).isNotNull();
		assertThat(found.getTitle()).isEqualTo(TITLE);
		assertThat(found.getContent()).isEqualTo(CONTENT);
	}

	@Test
	void 영상_객체_키로_발급_시_만든_분석을_찾는다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(uploadingAnalysis(7L, VIDEO_OBJECT_KEY));

		assertThat(aiAnalysisRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY))
				.map(AiAnalysis::getId).contains(saved.getId());
	}

	@Test
	void 분석이_없는_영상_객체_키면_결과가_비어_있다() {
		assertThat(aiAnalysisRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY)).isEmpty();
	}

	@Test
	void 사용자의_분석만_생성_시각_내림차순으로_조회한다() {
		AiAnalysis first = aiAnalysisRepository.saveAndFlush(
				completedAnalysis(7L, "videos/training/7/first.mp4", "첫 제목", "첫 리포트"));
		AiAnalysis second = aiAnalysisRepository.saveAndFlush(
				completedAnalysis(7L, "videos/training/7/second.mp4", "둘째 제목", "둘째 리포트"));
		aiAnalysisRepository.saveAndFlush(completedAnalysis(8L, "videos/training/8/other.mp4", "남의 제목", "남의 리포트"));

		List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L);

		assertThat(analyses).extracting(AiAnalysis::getId)
				.containsExactly(second.getId(), first.getId());
	}

	@Test
	void 분석이_없는_사용자의_완료_분석_수는_0이다() {
		assertThat(aiAnalysisRepository.countByUserIdAndStatus(7L, AiAnalysisStatus.COMPLETED)).isZero();
	}

	@Test
	void 사용자의_분석_중_주어진_상태인_것만_센다() {
		aiAnalysisRepository.saveAndFlush(completedAnalysis(7L, "videos/training/7/first.mp4", "첫 제목", "첫 리포트"));
		aiAnalysisRepository.saveAndFlush(completedAnalysis(7L, "videos/training/7/second.mp4", "둘째 제목", "둘째 리포트"));
		aiAnalysisRepository.saveAndFlush(failedAnalysis(7L, "videos/training/7/failed.mp4"));
		aiAnalysisRepository.saveAndFlush(pendingAnalysis(7L, "videos/training/7/pending.mp4"));
		aiAnalysisRepository.saveAndFlush(completedAnalysis(8L, "videos/training/8/other.mp4", "남의 제목", "남의 리포트"));

		assertThat(aiAnalysisRepository.countByUserIdAndStatus(7L, AiAnalysisStatus.COMPLETED)).isEqualTo(2);
	}

	@Test
	void 분석_ID와_소유자가_모두_일치할_때만_조회된다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(completedAnalysis(7L, VIDEO_OBJECT_KEY, TITLE, CONTENT));

		assertThat(aiAnalysisRepository.findByIdAndUserId(saved.getId(), 7L)).isPresent();
		assertThat(aiAnalysisRepository.findByIdAndUserId(saved.getId(), 8L)).isEmpty();
	}

	private static AiAnalysis uploadingAnalysis(Long userId, String videoObjectKey) {
		return AiAnalysis.uploading(new AiAnalysisUploadCommand(userId, videoObjectKey, UPLOAD_EXPIRES_AT));
	}

	private static AiAnalysis pendingAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = uploadingAnalysis(userId, videoObjectKey);
		analysis.startAnalysis();
		return analysis;
	}

	private static AiAnalysis completedAnalysis(Long userId, String videoObjectKey, String title, String content) {
		AiAnalysis analysis = pendingAnalysis(userId, videoObjectKey);
		analysis.complete(title, content);
		return analysis;
	}

	private static AiAnalysis failedAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = pendingAnalysis(userId, videoObjectKey);
		analysis.failByAnalysis();
		return analysis;
	}
}
