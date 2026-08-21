package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * AI 리포트 저장 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("AI 리포트 리포지토리")
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
	@DisplayName("완료된 리포트를 저장하고 다시 조회할 수 있다")
	void saveAndFindRoundTrips() {
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
	@DisplayName("제목이 없는 리포트도 저장하고 다시 조회할 수 있다")
	void saveAndFindRoundTripsWithoutTitle() {
		AiReport saved = aiReportRepository.saveAndFlush(completedReport(7L, VIDEO_OBJECT_KEY, null, CONTENT));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getTitle()).isNull();
	}

	@Test
	@DisplayName("본문이 없는 UPLOADING 리포트도 업로드 만료 시각과 함께 저장하고 다시 조회할 수 있다")
	void saveAndFindRoundTripsUploadingReportWithoutContent() {
		AiReport saved = aiReportRepository.saveAndFlush(uploadingReport(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
		assertThat(found.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(found.getTitle()).isNull();
		assertThat(found.getContent()).isNull();
	}

	@Test
	@DisplayName("준영속 리포트의 상태를 전이한 뒤 다시 저장하면 갱신된 상태로 조회된다")
	void saveDetachedReportAfterTransitionUpdatesStatus() {
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
	@DisplayName("영상 객체 키로 발급 시 만든 리포트를 찾는다")
	void findByVideoObjectKeyReturnsIssuedReport() {
		AiReport saved = aiReportRepository.saveAndFlush(uploadingReport(7L, VIDEO_OBJECT_KEY));

		assertThat(aiReportRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY))
				.map(AiReport::getId).contains(saved.getId());
	}

	@Test
	@DisplayName("리포트가 없는 영상 객체 키면 결과가 비어 있다")
	void findByVideoObjectKeyReturnsEmptyWhenReportIsAbsent() {
		assertThat(aiReportRepository.findByVideoObjectKey(VIDEO_OBJECT_KEY)).isEmpty();
	}

	@Test
	@DisplayName("사용자의 리포트만 생성 시각 내림차순으로 조회한다")
	void findAllByUserIdReturnsOwnReportsLatestFirst() {
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
	@DisplayName("리포트가 없는 사용자의 완료 리포트 수는 0이다")
	void countByUserIdAndStatusReturnsZeroWhenNoReportExists() {
		assertThat(aiReportRepository.countByUserIdAndStatus(7L, AiReportStatus.COMPLETED)).isZero();
	}

	@Test
	@DisplayName("사용자의 리포트 중 주어진 상태인 것만 센다")
	void countByUserIdAndStatusCountsOnlyOwnReportsInStatus() {
		aiReportRepository.saveAndFlush(completedReport(7L, "videos/training/7/first.mp4", "첫 제목", "첫 리포트"));
		aiReportRepository.saveAndFlush(completedReport(7L, "videos/training/7/second.mp4", "둘째 제목", "둘째 리포트"));
		aiReportRepository.saveAndFlush(analysisFailedReport(7L, "videos/training/7/failed.mp4"));
		aiReportRepository.saveAndFlush(pendingReport(7L, "videos/training/7/pending.mp4"));
		aiReportRepository.saveAndFlush(completedReport(8L, "videos/training/8/other.mp4", "남의 제목", "남의 리포트"));

		assertThat(aiReportRepository.countByUserIdAndStatus(7L, AiReportStatus.COMPLETED)).isEqualTo(2);
	}

	@Test
	@DisplayName("리포트 ID와 소유자가 모두 일치할 때만 조회된다")
	void findByIdAndUserIdReturnsEmptyForOtherUsersReport() {
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
