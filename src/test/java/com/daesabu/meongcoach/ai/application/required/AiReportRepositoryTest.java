package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * AI 리포트 저장 리포지토리 검증.
 */
@DataJpaTest
class AiReportRepositoryTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/7/key.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";
	private static final LocalDateTime UPLOAD_EXPIRES_AT = LocalDateTime.of(2026, 8, 21, 12, 15, 0);

	@Autowired
	private AiReportRepository aiReportRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 완료된_리포트를_저장하고_다시_조회할_수_있다() {
		AiReport saved = aiReportRepository.saveAndFlush(completedReport(7L, VIDEO_OBJECT_KEY, TITLE, CONTENT));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getUserId()).isEqualTo(7L);
		assertThat(found.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(found.getTitle()).isEqualTo(TITLE);
		assertThat(found.getContent()).isEqualTo(CONTENT);
		assertThat(found.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	void 제목이_없는_리포트도_저장하고_다시_조회할_수_있다() {
		AiReport saved = aiReportRepository.saveAndFlush(completedReport(7L, VIDEO_OBJECT_KEY, null, CONTENT));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getTitle()).isNull();
	}

	@Test
	void 본문이_없는_UPLOADING_리포트도_업로드_만료_시각과_함께_저장하고_다시_조회할_수_있다() {
		AiReport saved = aiReportRepository.saveAndFlush(uploadingReport(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
		assertThat(found.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(found.getTitle()).isNull();
		assertThat(found.getContent()).isNull();
	}

	@Test
	void 준영속_리포트의_상태를_전이한_뒤_다시_저장하면_갱신된_상태로_조회된다() {
		// 생성 서비스는 트랜잭션 없이 save(merge)로 상태를 전이하므로, 준영속 인스턴스의 재저장이 UPDATE로 반영돼야 한다
		AiReport saved = aiReportRepository.saveAndFlush(pendingReport(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		saved.failByAnalysis();
		aiReportRepository.saveAndFlush(saved);
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(AiReportStatus.FAILED_ANALYSIS);
	}

	@Test
	void 영상_객체_키로_발급_시_만든_리포트를_찾는다() {
		AiReport saved = aiReportRepository.saveAndFlush(uploadingReport(7L, VIDEO_OBJECT_KEY));

		assertThat(aiReportRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY))
				.map(AiReport::getId).contains(saved.getId());
	}

	@Test
	void 리포트가_없는_영상_객체_키면_결과가_비어_있다() {
		assertThat(aiReportRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY)).isEmpty();
	}

	@Test
	void 사용자의_리포트만_생성_시각_내림차순으로_조회한다() {
		AiReport first = aiReportRepository.saveAndFlush(
				completedReport(7L, "videos/training/7/first.mp4", "첫 제목", "첫 리포트"));
		AiReport second = aiReportRepository.saveAndFlush(
				completedReport(7L, "videos/training/7/second.mp4", "둘째 제목", "둘째 리포트"));
		aiReportRepository.saveAndFlush(completedReport(8L, "videos/training/8/other.mp4", "남의 제목", "남의 리포트"));

		List<AiReport> reports = aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L);

		assertThat(reports).extracting(AiReport::getId)
				.containsExactly(second.getId(), first.getId());
	}

	@Test
	void 리포트가_없는_사용자의_완료_리포트_수는_0이다() {
		assertThat(aiReportRepository.countByUserIdAndStatus(7L, AiReportStatus.COMPLETED)).isZero();
	}

	@Test
	void 사용자의_리포트_중_주어진_상태인_것만_센다() {
		aiReportRepository.saveAndFlush(completedReport(7L, "videos/training/7/first.mp4", "첫 제목", "첫 리포트"));
		aiReportRepository.saveAndFlush(completedReport(7L, "videos/training/7/second.mp4", "둘째 제목", "둘째 리포트"));
		aiReportRepository.saveAndFlush(analysisFailedReport(7L, "videos/training/7/failed.mp4"));
		aiReportRepository.saveAndFlush(pendingReport(7L, "videos/training/7/pending.mp4"));
		aiReportRepository.saveAndFlush(completedReport(8L, "videos/training/8/other.mp4", "남의 제목", "남의 리포트"));

		assertThat(aiReportRepository.countByUserIdAndStatus(7L, AiReportStatus.COMPLETED)).isEqualTo(2);
	}

	@Test
	void 리포트_ID와_소유자가_모두_일치할_때만_조회된다() {
		AiReport saved = aiReportRepository.saveAndFlush(completedReport(7L, VIDEO_OBJECT_KEY, TITLE, CONTENT));

		assertThat(aiReportRepository.findByIdAndUserId(saved.getId(), 7L)).isPresent();
		assertThat(aiReportRepository.findByIdAndUserId(saved.getId(), 8L)).isEmpty();
	}

	private static AiReport uploadingReport(Long userId, String videoObjectKey) {
		return AiReport.uploading(new AiReportUploadCommand(userId, videoObjectKey, UPLOAD_EXPIRES_AT));
	}

	private static AiReport pendingReport(Long userId, String videoObjectKey) {
		AiReport report = uploadingReport(userId, videoObjectKey);
		report.startAnalysis();
		return report;
	}

	private static AiReport completedReport(Long userId, String videoObjectKey, String title, String content) {
		AiReport report = pendingReport(userId, videoObjectKey);
		report.complete(title, content);
		return report;
	}

	private static AiReport analysisFailedReport(Long userId, String videoObjectKey) {
		AiReport report = pendingReport(userId, videoObjectKey);
		report.failByAnalysis();
		return report;
	}
}
