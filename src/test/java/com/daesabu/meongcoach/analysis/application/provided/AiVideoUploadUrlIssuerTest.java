package com.daesabu.meongcoach.analysis.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.analysis.application.provided.dto.AiVideoUploadUrlRequest;
import com.daesabu.meongcoach.analysis.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiAnalysis;
import com.daesabu.meongcoach.analysis.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.analysis.domain.AiTrialFixture;
import com.daesabu.meongcoach.analysis.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.analysis.domain.exception.AiVideoDurationOutOfRangeException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AiVideoUploadUrlIssuerTest {

	private static final Long USER_ID = 7L;
	private static final String GCS_BASE_URL = "https://storage.googleapis.com/test-ai-video-bucket/";
	private static final long EXPIRES_IN_SECONDS = 900L;
	private static final AiVideoUploadUrlRequest REQUEST = new AiVideoUploadUrlRequest("video/mp4", 10485760L, 60L);

	@Autowired
	private AiVideoUploadUrlIssuer service;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 29})
	void 체험_횟수가_남아_있으면_분석_저장소의_훈련_영상_업로드_URL_발급을_위임한다(int usedCount) {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(USER_ID, usedCount));

		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		assertThat(result.objectKey()).matches("videos/training/7/[0-9a-f-]{36}\\.mp4");
		assertThat(result.uploadUrl())
				.startsWith(GCS_BASE_URL + result.objectKey())
				.contains("X-Goog-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(result.publicUrl()).isEqualTo(GCS_BASE_URL + result.objectKey());
		assertThat(result.expiresInSeconds()).isEqualTo(EXPIRES_IN_SECONDS);
	}

	@Test
	void 발급과_함께_UPLOADING_리포트를_저장하고_그_ID를_돌려준다() {
		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		AiAnalysis saved = aiAnalysisRepository.findById(result.reportId()).orElseThrow();
		assertThat(saved.getUserId()).isEqualTo(USER_ID);
		assertThat(saved.getVideoObjectKey()).isEqualTo(result.objectKey());
		assertThat(saved.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
	}

	@Test
	void 업로드_만료_시각은_발급_시각에_URL_유효_시간을_더한_값이다() {
		Instant before = Instant.now();

		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		Instant after = Instant.now();
		AiAnalysis saved = aiAnalysisRepository.findById(result.reportId()).orElseThrow();
		assertThat(saved.getUploadExpiresAt())
				.isBetween(before.plusSeconds(EXPIRES_IN_SECONDS), after.plusSeconds(EXPIRES_IN_SECONDS));
	}

	@Test
	void 체험_횟수를_소진했으면_URL_발급과_리포트_저장_없이_예외를_던진다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(USER_ID, AiTrial.DEFAULT_GRANTED_COUNT));

		assertThatThrownBy(() -> service.issue(USER_ID, REQUEST))
				.isInstanceOf(AiReportTrialExceededException.class);
		assertThat(aiAnalysisRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(USER_ID)).isEmpty();
	}

	@Test
	void 저장된_현황이_없는_사용자는_체험이_남아_있어_URL을_발급받는다() {
		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		assertThat(result.reportId()).isNotNull();
	}

	@Test
	void 부여_횟수가_늘어난_사용자는_기본_횟수를_다_써도_URL을_발급받는다() {
		aiTrialRepository.saveAndFlush(
				AiTrialFixture.create(USER_ID, AiTrial.DEFAULT_GRANTED_COUNT + 1, AiTrial.DEFAULT_GRANTED_COUNT));

		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		assertThat(result.reportId()).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(longs = {9L, 301L})
	void 영상_길이가_허용_범위를_벗어나면_URL_발급과_리포트_저장_없이_예외를_던진다(long durationSeconds) {
		AiVideoUploadUrlRequest request = new AiVideoUploadUrlRequest("video/mp4", 10485760L, durationSeconds);

		assertThatThrownBy(() -> service.issue(USER_ID, request))
				.isInstanceOf(AiVideoDurationOutOfRangeException.class);
		assertThat(aiAnalysisRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(USER_ID)).isEmpty();
	}

	@Test
	void 영상_길이가_없으면_검증에_실패한다() {
		AiVideoUploadUrlRequest request = new AiVideoUploadUrlRequest("video/mp4", 10485760L, null);

		assertThatThrownBy(() -> service.issue(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 체험_현황은_요청한_사용자_기준으로_조회하므로_다른_사용자가_소진해도_발급한다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(99L, AiTrial.DEFAULT_GRANTED_COUNT));

		AiVideoUploadUrlResult result = service.issue(USER_ID, REQUEST);

		assertThat(result.reportId()).isNotNull();
	}

}
