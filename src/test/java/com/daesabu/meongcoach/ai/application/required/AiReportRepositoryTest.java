package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
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

	@Autowired
	private AiReportRepository aiReportRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("리포트를 저장하고 다시 조회할 수 있다")
	void saveAndFindRoundTrips() {
		AiReport saved = aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(7L, VIDEO_OBJECT_KEY, "분리불안 징후가 관찰됩니다.")));
		entityManager.clear();

		AiReport found = aiReportRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getUserId()).isEqualTo(7L);
		assertThat(found.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(found.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("같은 영상 객체 키의 리포트가 있으면 존재한다고 알려준다")
	void existsByVideoObjectKeyReturnsTrueWhenReportExists() {
		aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(7L, VIDEO_OBJECT_KEY, "분리불안 징후가 관찰됩니다.")));

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
				AiReport.create(new AiReportCreateCommand(7L, "videos/training/7/first.mp4", "첫 리포트")));
		AiReport second = aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(7L, "videos/training/7/second.mp4", "둘째 리포트")));
		aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(8L, "videos/training/8/other.mp4", "남의 리포트")));

		List<AiReport> reports = aiReportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L);

		assertThat(reports).extracting(AiReport::getId)
				.containsExactly(second.getId(), first.getId());
	}

	@Test
	@DisplayName("리포트 ID와 소유자가 모두 일치할 때만 조회된다")
	void findByIdAndUserIdReturnsEmptyForOtherUsersReport() {
		AiReport saved = aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(7L, VIDEO_OBJECT_KEY, "분리불안 징후가 관찰됩니다.")));

		assertThat(aiReportRepository.findByIdAndUserId(saved.getId(), 7L)).isPresent();
		assertThat(aiReportRepository.findByIdAndUserId(saved.getId(), 8L)).isEmpty();
	}
}
