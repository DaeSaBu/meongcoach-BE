package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("AI 리포트 체험 조회 서비스")
class AiReportTrialFinderServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Autowired
	private AiReportRepository aiReportRepository;

	@Test
	@DisplayName("리포트가 없으면 사용 0회로 조회된다")
	void findTrialReturnsZeroUsedCountWhenNoReportExists() {
		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	@Test
	@DisplayName("생성한 리포트 수가 그대로 사용 횟수가 된다")
	void findTrialCountsGeneratedReports() {
		persistReport(USER_ID, "videos/training/42/first.mp4");
		persistReport(USER_ID, "videos/training/42/second.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(2));
	}

	@Test
	@DisplayName("다른 사용자의 리포트는 사용 횟수에 세지 않는다")
	void findTrialIgnoresOtherUsersReports() {
		persistReport(OTHER_USER_ID, "videos/training/99/other.mp4");

		assertThat(aiTrialFinder.findTrial(USER_ID)).isEqualTo(new AiTrial(0));
	}

	private void persistReport(Long userId, String videoObjectKey) {
		aiReportRepository.saveAndFlush(
				AiReport.create(new AiReportCreateCommand(userId, videoObjectKey, "분리불안 징후가 관찰됩니다.")));
	}
}
