package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AiReportTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/1/video.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 21, 12, 0, 0);
	private static final LocalDateTime UPLOAD_EXPIRES_AT = NOW.plusMinutes(15);

	@Test
	void UPLOADING으로_생성하면_사용자_영상_객체_키_업로드_만료_시각이_설정되고_제목_본문은_비어_있다() {
		AiReport report = uploadingReport();

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(report.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(report.getTitle()).isNull();
		assertThat(report.getContent()).isNull();
	}

	@Test
	void UPLOADING으로_생성하면_상태가_UPLOADING이다() {
		AiReport report = uploadingReport();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
		assertThat(report.isUploading()).isTrue();
	}

	@Test
	void 분석을_시작하면_상태가_PENDING이_된다() {
		AiReport report = uploadingReport();

		report.startAnalysis();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.PENDING);
		assertThat(report.isUploading()).isFalse();
	}

	@Test
	void 업로드_만료_전의_UPLOADING은_조회_시점_상태도_UPLOADING이다() {
		AiReport report = uploadingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT)).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	void 업로드가_만료된_UPLOADING은_조회_시점_상태가_FAILED_UPLOAD다() {
		AiReport report = uploadingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT.plusSeconds(1))).isEqualTo(AiReportStatus.FAILED_UPLOAD);
		// 만료는 DB 상태를 바꾸지 않는다
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	void UPLOADING이_아닌_리포트는_만료_시각이_지나도_조회_시점_상태가_그대로다() {
		AiReport report = pendingReport();

		assertThat(report.statusAt(UPLOAD_EXPIRES_AT.plusDays(1))).isEqualTo(AiReportStatus.PENDING);
	}

	@Test
	void 완료하면_제목_본문이_채워지고_상태가_COMPLETED가_된다() {
		AiReport report = pendingReport();

		report.complete(TITLE, CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
		assertThat(report.getContent()).isEqualTo(CONTENT);
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	void 제목_없이도_완료할_수_있다() {
		AiReport report = pendingReport();

		report.complete(null, CONTENT);

		assertThat(report.getTitle()).isNull();
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	void 컬럼_길이를_넘는_제목은_잘라서_저장한다() {
		// 제목은 부가 정보라 길이 위반으로 리포트 저장 전체가 실패해서는 안 된다
		AiReport report = pendingReport();

		report.complete("가".repeat(500), CONTENT);

		assertThat(report.getTitle()).hasSize(AiReport.TITLE_MAX_LENGTH);
	}

	@Test
	void 공백뿐인_제목은_null로_저장한다() {
		AiReport report = pendingReport();

		report.complete("   ", CONTENT);

		assertThat(report.getTitle()).isNull();
	}

	@Test
	void 제목_앞뒤_공백은_제거해_저장한다() {
		AiReport report = pendingReport();

		report.complete("  " + TITLE + "  ", CONTENT);

		assertThat(report.getTitle()).isEqualTo(TITLE);
	}

	@Test
	void 체험_횟수_초과로_실패_처리하면_상태가_FAILED_TRIAL_EXCEEDED가_된다() {
		AiReport report = pendingReport();

		report.failByTrialExceeded();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	void 분석_실패로_실패_처리하면_상태가_FAILED_ANALYSIS가_된다() {
		AiReport report = pendingReport();

		report.failByAnalysis();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_ANALYSIS);
	}

	@Test
	void 예상하지_못한_예외로_실패_처리하면_상태가_FAILED_UNEXPECTED가_된다() {
		AiReport report = pendingReport();

		report.failUnexpectedly();

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 실패_처리해도_본문은_채워지지_않는다() {
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
