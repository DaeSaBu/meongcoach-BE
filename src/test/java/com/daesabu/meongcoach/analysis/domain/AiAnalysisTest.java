package com.daesabu.meongcoach.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AiAnalysisTest {

	private static final String VIDEO_OBJECT_KEY = "videos/training/1/video.mp4";
	private static final String TITLE = "분리불안 징후 행동 분석";
	private static final String CONTENT = "분리불안 징후가 관찰됩니다.";
	private static final Instant UPLOAD_EXPIRES_AT = Instant.parse("2026-08-21T12:15:00Z");

	@Test
	void UPLOADING으로_생성하면_사용자와_영상_객체_키_업로드_만료_시각이_설정되고_결과는_없다() {
		AiAnalysis analysis = uploadingAnalysis();

		assertThat(analysis.getUserId()).isEqualTo(1L);
		assertThat(analysis.getVideoObjectKey()).isEqualTo(VIDEO_OBJECT_KEY);
		assertThat(analysis.getUploadExpiresAt()).isEqualTo(UPLOAD_EXPIRES_AT);
		assertThat(analysis.getReport()).isNull();
		assertThat(analysis.getTitle()).isNull();
		assertThat(analysis.getContent()).isNull();
	}

	@Test
	void 보호자가_남긴_맥락을_앞뒤_공백을_지워_저장한다() {
		AiAnalysis analysis = AiAnalysis.uploading(
				new AiAnalysisUploadCommand(1L, VIDEO_OBJECT_KEY, UPLOAD_EXPIRES_AT, "  퇴근하면 짖어요  "));

		assertThat(analysis.getContext()).isEqualTo("퇴근하면 짖어요");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   "})
	void 맥락이_비어_있으면_null로_저장한다(String context) {
		AiAnalysis analysis = AiAnalysis.uploading(
				new AiAnalysisUploadCommand(1L, VIDEO_OBJECT_KEY, UPLOAD_EXPIRES_AT, context));

		assertThat(analysis.getContext()).isNull();
	}

	@Test
	void UPLOADING으로_생성하면_상태가_UPLOADING이다() {
		AiAnalysis analysis = uploadingAnalysis();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
		assertThat(analysis.isUploading()).isTrue();
	}

	@Test
	void 분석을_시작하면_상태가_ANALYZING이_된다() {
		AiAnalysis analysis = uploadingAnalysis();

		analysis.startAnalysis();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.ANALYZING);
		assertThat(analysis.isUploading()).isFalse();
	}

	@Test
	void 업로드_만료_전의_UPLOADING은_조회_시점_상태도_UPLOADING이다() {
		AiAnalysis analysis = uploadingAnalysis();

		assertThat(analysis.statusAt(UPLOAD_EXPIRES_AT)).isEqualTo(AiAnalysisStatus.UPLOADING);
	}

	@Test
	void 업로드가_만료된_UPLOADING은_조회_시점_상태가_FAILED_UPLOAD이고_저장_상태는_그대로다() {
		AiAnalysis analysis = uploadingAnalysis();

		assertThat(analysis.statusAt(UPLOAD_EXPIRES_AT.plusSeconds(1))).isEqualTo(AiAnalysisStatus.FAILED_UPLOAD);
		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
	}

	@Test
	void UPLOADING이_아닌_분석은_만료_시각이_지나도_조회_시점_상태가_그대로다() {
		AiAnalysis analysis = startedAnalysis();

		assertThat(analysis.statusAt(UPLOAD_EXPIRES_AT.plusSeconds(86400))).isEqualTo(AiAnalysisStatus.ANALYZING);
	}

	@Test
	void 완료하면_리포트가_생기고_상태가_COMPLETED가_된다() {
		AiAnalysis analysis = startedAnalysis();

		analysis.complete(TITLE, CONTENT);

		assertThat(analysis.getReport()).isNotNull();
		assertThat(analysis.getTitle()).isEqualTo(TITLE);
		assertThat(analysis.getContent()).isEqualTo(CONTENT);
		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 제목_없이도_완료할_수_있다() {
		AiAnalysis analysis = startedAnalysis();

		analysis.complete(null, CONTENT);

		assertThat(analysis.getTitle()).isNull();
		assertThat(analysis.getContent()).isEqualTo(CONTENT);
		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 체험_횟수_초과로_실패_처리하면_상태가_FAILED_TRIAL_EXCEEDED가_되고_리포트가_없다() {
		AiAnalysis analysis = startedAnalysis();

		analysis.failByTrialExceeded();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED_TRIAL_EXCEEDED);
		assertThat(analysis.getReport()).isNull();
	}

	@Test
	void 분석_실패로_실패_처리하면_상태가_FAILED_ANALYSIS가_되고_리포트가_없다() {
		AiAnalysis analysis = startedAnalysis();

		analysis.failByAnalysis();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED_ANALYSIS);
		assertThat(analysis.getReport()).isNull();
	}

	@Test
	void 예상하지_못한_예외로_실패_처리하면_상태가_FAILED_UNEXPECTED가_된다() {
		AiAnalysis analysis = startedAnalysis();

		analysis.failUnexpectedly();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 완료_후_예상하지_못한_예외로_실패_처리하면_리포트를_버린다() {
		AiAnalysis analysis = startedAnalysis();
		analysis.complete(TITLE, CONTENT);

		analysis.failUnexpectedly();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED_UNEXPECTED);
		assertThat(analysis.getReport()).isNull();
	}

	private static AiAnalysis uploadingAnalysis() {
		return AiAnalysis.uploading(new AiAnalysisUploadCommand(1L, VIDEO_OBJECT_KEY, UPLOAD_EXPIRES_AT, null));
	}

	private static AiAnalysis startedAnalysis() {
		AiAnalysis analysis = uploadingAnalysis();
		analysis.startAnalysis();
		return analysis;
	}
}
