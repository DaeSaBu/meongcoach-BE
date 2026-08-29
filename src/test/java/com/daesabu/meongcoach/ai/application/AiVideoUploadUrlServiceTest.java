package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 발급과 함께 UPLOADING 리포트가 저장되는지까지 보려고 실제 DB 슬라이스를 쓴다. 체험 횟수도 실제 COMPLETED row 수로 센다.
 */
@DataJpaTest
@Import({AiVideoUploadUrlService.class, AiReportTrialFinderService.class})
class AiVideoUploadUrlServiceTest {

	private static final Long USER_ID = 7L;
	private static final String UPLOAD_URL = "https://storage.test/upload?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://videos.test/videos/training/7/key.mp4";
	private static final String OBJECT_KEY = "videos/training/7/key.mp4";
	private static final long EXPIRES_IN_SECONDS = 900L;

	@Autowired
	private AiVideoUploadUrlIssuer service;

	@Autowired
	private AiReportRepository aiReportRepository;

	@Autowired
	private RecordingVideoUploadUrlIssuer videoUploadUrlIssuer;

	@TestConfiguration
	static class MediaFakeConfig {

		@Bean
		RecordingVideoUploadUrlIssuer videoUploadUrlIssuer() {
			return new RecordingVideoUploadUrlIssuer();
		}
	}

	@BeforeEach
	void setUp() {
		videoUploadUrlIssuer.issuedRequests.clear();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	void 체험_횟수가_남아_있으면_훈련_영상_대상으로_업로드_URL_발급을_위임한다(int usedCount) {
		persistCompletedReports(usedCount);

		AiVideoUploadUrlResult result = service.issue(USER_ID, "video/mp4", 10485760L);

		assertThat(videoUploadUrlIssuer.issuedRequests).containsExactly("7:TRAINING_VIDEO:video/mp4:10485760");
		assertThat(result.uploadUrl()).isEqualTo(UPLOAD_URL);
		assertThat(result.publicUrl()).isEqualTo(PUBLIC_URL);
		assertThat(result.objectKey()).isEqualTo(OBJECT_KEY);
		assertThat(result.expiresInSeconds()).isEqualTo(EXPIRES_IN_SECONDS);
	}

	@Test
	void 발급과_함께_UPLOADING_리포트를_저장하고_그_ID를_돌려준다() {
		AiVideoUploadUrlResult result = service.issue(USER_ID, "video/mp4", 10485760L);

		AiReport saved = aiReportRepository.findById(result.reportId()).orElseThrow();
		assertThat(saved.getUserId()).isEqualTo(USER_ID);
		assertThat(saved.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(saved.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	void 업로드_만료_시각은_발급_시각에_URL_유효_시간을_더한_값이다() {
		LocalDateTime before = LocalDateTime.now();

		AiVideoUploadUrlResult result = service.issue(USER_ID, "video/mp4", 10485760L);

		LocalDateTime after = LocalDateTime.now();
		AiReport saved = aiReportRepository.findById(result.reportId()).orElseThrow();
		assertThat(saved.getUploadExpiresAt())
				.isBetween(before.plusSeconds(EXPIRES_IN_SECONDS), after.plusSeconds(EXPIRES_IN_SECONDS));
	}

	@Test
	void 체험_횟수를_소진했으면_URL_발급과_리포트_저장_없이_예외를_던진다() {
		persistCompletedReports(AiTrial.MAX_COUNT);

		assertThatThrownBy(() -> service.issue(USER_ID, "video/mp4", 10485760L))
				.isInstanceOf(AiReportTrialExceededException.class);
		assertThat(videoUploadUrlIssuer.issuedRequests).isEmpty();
		assertThat(aiReportRepository.findByVideoObjectKey(OBJECT_KEY)).isEmpty();
	}

	@Test
	void 체험_현황은_요청한_사용자_기준으로_조회하므로_다른_사용자의_완료_리포트는_세지_않는다() {
		for (int i = 0; i < AiTrial.MAX_COUNT; i++) {
			persistCompletedReport(99L, "videos/training/99/" + i + ".mp4");
		}

		AiVideoUploadUrlResult result = service.issue(USER_ID, "video/mp4", 10485760L);

		assertThat(result.reportId()).isNotNull();
	}

	private void persistCompletedReports(int count) {
		for (int i = 0; i < count; i++) {
			persistCompletedReport(USER_ID, "videos/training/7/" + i + ".mp4");
		}
	}

	private void persistCompletedReport(Long userId, String videoObjectKey) {
		AiReport report = AiReport.uploading(
				new AiReportUploadCommand(userId, videoObjectKey, LocalDateTime.now().plusMinutes(15)));
		report.startAnalysis();
		report.complete("제목", "본문");
		aiReportRepository.saveAndFlush(report);
	}

	static class RecordingVideoUploadUrlIssuer implements VideoUploadUrlIssuer {

		private final List<String> issuedRequests = new ArrayList<>();

		@Override
		public VideoUploadUrlResult issue(Long userId, String target, String contentType, long fileSizeBytes) {
			issuedRequests.add(userId + ":" + target + ":" + contentType + ":" + fileSizeBytes);
			return new VideoUploadUrlResult(UPLOAD_URL, PUBLIC_URL, OBJECT_KEY, EXPIRES_IN_SECONDS);
		}
	}
}
