package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
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
	@DisplayName("본문이 없는 PENDING 리포트도 저장하고 다시 조회할 수 있다")
	void saveAndFindRoundTripsPendingReportWithoutContent() {
		AiReport saved = aiReportRepository.saveAndFlush(AiReport.pending(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(AiReportStatus.PENDING);
		assertThat(found.getTitle()).isNull();
		assertThat(found.getContent()).isNull();
	}

	@Test
	@DisplayName("준영속 리포트의 상태를 전이한 뒤 다시 저장하면 갱신된 상태로 조회된다")
	void saveDetachedReportAfterTransitionUpdatesStatus() {
		// 생성 서비스는 트랜잭션 없이 save(merge)로 상태를 전이하므로, 준영속 인스턴스의 재저장이 UPDATE로 반영돼야 한다
		AiReport saved = aiReportRepository.saveAndFlush(AiReport.pending(7L, VIDEO_OBJECT_KEY));
		entityManager.clear();

		saved.fail(AiReportStatus.FAILED_ANALYSIS);
		aiReportRepository.saveAndFlush(saved);
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(AiReportStatus.FAILED_ANALYSIS);
	}

	@Test
	@DisplayName("같은 영상 객체 키의 리포트가 있으면 존재한다고 알려준다")
	void existsByVideoObjectKeyReturnsTrueWhenReportExists() {
		aiReportRepository.saveAndFlush(completedReport(7L, VIDEO_OBJECT_KEY, TITLE, CONTENT));

		assertThat(aiReportRepository.existsByVideoObjectKey(VIDEO_OBJECT_KEY)).isTrue();
	}

	@Test
	@DisplayName("실패한 리포트의 영상 객체 키도 존재한다고 알려준다")
	void existsByVideoObjectKeyReturnsTrueForFailedReport() {
		// 상태를 가리지 않으므로 실패한 영상은 재분석되지 않는다
		aiReportRepository.saveAndFlush(failedReport(7L, VIDEO_OBJECT_KEY, AiReportStatus.FAILED_ANALYSIS));

		assertThat(aiReportRepository.existsByVideoObjectKey(VIDEO_OBJECT_KEY)).isTrue();
	}

	@Test
	@DisplayName("리포트가 없는 영상 객체 키면 존재하지 않는다고 알려준다")
	void existsByVideoObjectKeyReturnsFalseWhenReportIsAbsent() {
		assertThat(aiReportRepository.existsByVideoObjectKey(VIDEO_OBJECT_KEY)).isFalse();
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
		aiReportRepository.saveAndFlush(failedReport(7L, "videos/training/7/failed.mp4", AiReportStatus.FAILED_ANALYSIS));
		aiReportRepository.saveAndFlush(AiReport.pending(7L, "videos/training/7/pending.mp4"));
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

	private static AiReport completedReport(Long userId, String videoObjectKey, String title, String content) {
		AiReport report = AiReport.pending(userId, videoObjectKey);
		report.complete(title, content);
		return report;
	}

	private static AiReport failedReport(Long userId, String videoObjectKey, AiReportStatus status) {
		AiReport report = AiReport.pending(userId, videoObjectKey);
		report.fail(status);
		return report;
	}
}
