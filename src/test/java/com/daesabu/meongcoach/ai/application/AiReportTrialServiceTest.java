package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.provided.AiReportTrialView;
import com.daesabu.meongcoach.ai.application.provided.AiReportVideoUploadUrlView;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AI 리포트 체험 서비스")
class AiReportTrialServiceTest {

	private static final String UPLOAD_URL = "https://storage.test/upload?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://videos.test/videos/training/7/key.mp4";
	private static final String OBJECT_KEY = "videos/training/7/key.mp4";

	private AiReportRepository aiReportRepository;
	private RecordingVideoUploadUrlIssuer videoUploadUrlIssuer;
	private AiReportTrialService service;

	@BeforeEach
	void setUp() {
		aiReportRepository = mock(AiReportRepository.class);
		videoUploadUrlIssuer = new RecordingVideoUploadUrlIssuer();
		service = new AiReportTrialService(aiReportRepository, videoUploadUrlIssuer);
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1, 2})
	@DisplayName("생성된 리포트가 한도 미만이면 훈련 영상 대상으로 업로드 URL 발급을 위임한다")
	void issueDelegatesToMediaWhenTrialRemains(long generatedCount) {
		when(aiReportRepository.countByUserId(7L)).thenReturn(generatedCount);

		AiReportVideoUploadUrlView view = service.issue(7L, "video/mp4", 10485760L);

		assertThat(videoUploadUrlIssuer.issuedRequests).containsExactly("7:TRAINING_VIDEO:video/mp4:10485760");
		assertThat(view.uploadUrl()).isEqualTo(UPLOAD_URL);
		assertThat(view.publicUrl()).isEqualTo(PUBLIC_URL);
		assertThat(view.objectKey()).isEqualTo(OBJECT_KEY);
		assertThat(view.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	@DisplayName("체험 횟수를 소진했으면 URL 발급 없이 예외를 던진다")
	void issueThrowsWithoutDelegationWhenTrialExhausted() {
		when(aiReportRepository.countByUserId(7L)).thenReturn(3L);

		assertThatThrownBy(() -> service.issue(7L, "video/mp4", 10485760L))
				.isInstanceOf(AiReportTrialExceededException.class);
		assertThat(videoUploadUrlIssuer.issuedRequests).isEmpty();
	}

	@Test
	@DisplayName("리포트가 없으면 사용 0회·잔여 3회로 조회된다")
	void findTrialReturnsFullRemainingWhenNoReportExists() {
		when(aiReportRepository.countByUserId(7L)).thenReturn(0L);

		AiReportTrialView view = service.findTrial(7L);

		assertThat(view).isEqualTo(new AiReportTrialView(0, 3, 3));
	}

	@Test
	@DisplayName("생성한 리포트 수만큼 잔여 횟수가 줄어든다")
	void findTrialReturnsRemainingCountAfterUse() {
		when(aiReportRepository.countByUserId(7L)).thenReturn(2L);

		AiReportTrialView view = service.findTrial(7L);

		assertThat(view).isEqualTo(new AiReportTrialView(2, 3, 1));
	}

	@Test
	@DisplayName("한도를 넘겨 저장된 경우에도 잔여 횟수는 0으로 내려간다")
	void findTrialClampsRemainingCountToZero() {
		when(aiReportRepository.countByUserId(7L)).thenReturn(4L);

		AiReportTrialView view = service.findTrial(7L);

		assertThat(view).isEqualTo(new AiReportTrialView(4, 3, 0));
	}

	private static class RecordingVideoUploadUrlIssuer implements VideoUploadUrlIssuer {

		private final List<String> issuedRequests = new ArrayList<>();

		@Override
		public VideoUploadUrlResult issue(Long userId, String target, String contentType, long fileSizeBytes) {
			issuedRequests.add(userId + ":" + target + ":" + contentType + ":" + fileSizeBytes);
			return new VideoUploadUrlResult(UPLOAD_URL, PUBLIC_URL, OBJECT_KEY, 900L);
		}
	}
}
