package com.daesabu.meongcoach.ai.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
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
}
