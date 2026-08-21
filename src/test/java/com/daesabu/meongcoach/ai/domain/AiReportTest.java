package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReport 도메인")
class AiReportTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/1/video.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";

	@Test
	@DisplayName("PENDING으로 생성하면 사용자·영상 객체 키가 설정되고 제목·본문은 비어 있다")
	void pendingSetsUserAndVideoObjectKeyWithoutTitleAndContent() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(report.getTitle()).isNull();
		assertThat(report.getContent()).isNull();
	}

	@Test
	@DisplayName("PENDING으로 생성하면 상태가 PENDING이다")
	void pendingSetsStatusPending() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.PENDING);
	}

	@Test
	@DisplayName("완료하면 제목·본문이 채워지고 상태가 COMPLETED가 된다")
	void completeSetsTitleContentAndStatusCompleted() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.complete(TITLE, CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
		assertThat(report.getContent()).isEqualTo(CONTENT);
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("제목 없이도 완료할 수 있다")
	void completeAllowsNullTitle() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.complete(null, CONTENT);

		assertThat(report.getTitle()).isNull();
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("컬럼 길이를 넘는 제목은 잘라서 저장한다")
	void completeTruncatesTitleOverColumnLength() {
		// 제목은 부가 정보라 길이 위반으로 리포트 저장 전체가 실패해서는 안 된다
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.complete("가".repeat(500), CONTENT);

		assertThat(report.getTitle()).hasSize(AiReport.TITLE_MAX_LENGTH);
	}

	@Test
	@DisplayName("공백뿐인 제목은 null로 저장한다")
	void completeNormalizesBlankTitleToNull() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.complete("   ", CONTENT);

		assertThat(report.getTitle()).isNull();
	}

	@Test
	@DisplayName("제목 앞뒤 공백은 제거해 저장한다")
	void completeStripsTitleWhitespace() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.complete("  " + TITLE + "  ", CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
	}

	@Test
	@DisplayName("체험 횟수 초과로 실패 처리하면 상태가 FAILED_TRIAL_EXCEEDED가 된다")
	void failByTrialExceededSetsStatus() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.failByTrialExceeded();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	@DisplayName("분석 실패로 실패 처리하면 상태가 FAILED_ANALYSIS가 된다")
	void failByAnalysisSetsStatus() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.failByAnalysis();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_ANALYSIS);
	}

	@Test
	@DisplayName("예상하지 못한 예외로 실패 처리하면 상태가 FAILED_UNEXPECTED가 된다")
	void failUnexpectedlySetsStatus() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.failUnexpectedly();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 처리해도 본문은 채워지지 않는다")
	void failKeepsContentEmpty() {
		AiReport report = AiReport.pending(1L, VIDEO_OBJECT_KEY);

		report.failByAnalysis();

		assertThat(report.getContent()).isNull();
	}
}
