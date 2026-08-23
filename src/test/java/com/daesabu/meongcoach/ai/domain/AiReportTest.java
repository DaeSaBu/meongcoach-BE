package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReport 도메인")
class AiReportTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/1/video.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 21, 12, 0, 0);
	private static final LocalDateTime UPLOAD_EXPIRES_AT = NOW.plusMinutes(15);

	@Test
	@DisplayName("UPLOADING으로 생성하면 사용자·영상 객체 키·업로드 만료 시각이 설정되고 제목·본문은 비어 있다")
	void uploadingSetsUserKeyAndExpiryWithoutTitleAndContent() {
		AiReport report = uploadingReport();

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(report.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(report.getTitle()).isNull();
		assertThat(report.getContent()).isNull();
	}

	@Test
	@DisplayName("UPLOADING으로 생성하면 상태가 UPLOADING이다")
	void uploadingSetsStatusUploading() {
		AiReport report = uploadingReport();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
		assertThat(report.isUploading()).isTrue();
	}

	@Test
	@DisplayName("분석을 시작하면 상태가 PENDING이 된다")
	void startAnalysisSetsStatusPending() {
		AiReport report = uploadingReport();

		report.startAnalysis();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.PENDING);
		assertThat(report.isUploading()).isFalse();
	}

	@Test
	@DisplayName("업로드 만료 전의 UPLOADING은 조회 시점 상태도 UPLOADING이다")
	void statusAtKeepsUploadingBeforeExpiry() {
		AiReport report = uploadingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT)).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	@DisplayName("업로드가 만료된 UPLOADING은 조회 시점 상태가 FAILED_UPLOAD다")
	void statusAtDerivesFailedUploadAfterExpiry() {
		AiReport report = uploadingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT.plusSeconds(1))).isEqualTo(AiReportStatus.FAILED_UPLOAD);
		// 만료는 DB 상태를 바꾸지 않는다
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	@DisplayName("UPLOADING이 아닌 리포트는 만료 시각이 지나도 조회 시점 상태가 그대로다")
	void statusAtLeavesNonUploadingStatusUntouched() {
		AiReport report = pendingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT.plusDays(1))).isEqualTo(AiReportStatus.PENDING);
	}

	@Test
	@DisplayName("완료하면 제목·본문이 채워지고 상태가 COMPLETED가 된다")
	void completeSetsTitleContentAndStatusCompleted() {
		AiReport report = pendingReport();

		report.complete(TITLE, CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
		assertThat(report.getContent()).isEqualTo(CONTENT);
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("제목 없이도 완료할 수 있다")
	void completeAllowsNullTitle() {
		AiReport report = pendingReport();

		report.complete(null, CONTENT);

		assertThat(report.getTitle()).isNull();
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("컬럼 길이를 넘는 제목은 잘라서 저장한다")
	void completeTruncatesTitleOverColumnLength() {
		// 제목은 부가 정보라 길이 위반으로 리포트 저장 전체가 실패해서는 안 된다
		AiReport report = pendingReport();

		report.complete("가".repeat(500), CONTENT);

		assertThat(report.getTitle()).hasSize(AiReport.TITLE_MAX_LENGTH);
	}

	@Test
	@DisplayName("공백뿐인 제목은 null로 저장한다")
	void completeNormalizesBlankTitleToNull() {
		AiReport report = pendingReport();

		report.complete("   ", CONTENT);

		assertThat(report.getTitle()).isNull();
	}

	@Test
	@DisplayName("제목 앞뒤 공백은 제거해 저장한다")
	void completeStripsTitleWhitespace() {
		AiReport report = pendingReport();

		report.complete("  " + TITLE + "  ", CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
	}

	@Test
	@DisplayName("체험 횟수 초과로 실패 처리하면 상태가 FAILED_TRIAL_EXCEEDED가 된다")
	void failByTrialExceededSetsStatus() {
		AiReport report = pendingReport();

		report.failByTrialExceeded();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	@DisplayName("분석 실패로 실패 처리하면 상태가 FAILED_ANALYSIS가 된다")
	void failByAnalysisSetsStatus() {
		AiReport report = pendingReport();

		report.failByAnalysis();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_ANALYSIS);
	}

	@Test
	@DisplayName("예상하지 못한 예외로 실패 처리하면 상태가 FAILED_UNEXPECTED가 된다")
	void failUnexpectedlySetsStatus() {
		AiReport report = pendingReport();

		report.failUnexpectedly();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 처리해도 본문은 채워지지 않는다")
	void failKeepsContentEmpty() {
		AiReport report = pendingReport();

		report.failByAnalysis();

		assertThat(report.getContent()).isNull();
	}

	private static AiReport uploadingReport() {
		return AiReport.uploading(new AiReportUploadCommand(1L, VIDEO_OBJECT_KEY, UPLOAD_EXPIRES_AT));
	}

	private static AiReport pendingReport() {
		AiReport report = uploadingReport();
		report.startAnalysis();
		return report;
	}
}
