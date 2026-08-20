package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AI 영상 업로드 URL 발급 서비스")
class AiVideoUploadUrlServiceTest {

	private static final String UPLOAD_URL = "https://storage.test/upload?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://videos.test/videos/training/7/key.mp4";
	private static final String OBJECT_KEY = "videos/training/7/key.mp4";

	private StubAiTrialFinder aiTrialFinder;
	private RecordingVideoUploadUrlIssuer videoUploadUrlIssuer;
	private AiVideoUploadUrlService service;

	@BeforeEach
	void setUp() {
		aiTrialFinder = new StubAiTrialFinder();
		videoUploadUrlIssuer = new RecordingVideoUploadUrlIssuer();
		service = new AiVideoUploadUrlService(videoUploadUrlIssuer, aiTrialFinder);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	@DisplayName("체험 횟수가 남아 있으면 훈련 영상 대상으로 업로드 URL 발급을 위임한다")
	void issueDelegatesToMediaWhenTrialRemains(int usedCount) {
		aiTrialFinder.usedCount = usedCount;

		AiVideoUploadUrlResult result = service.issue(7L, "video/mp4", 10485760L);

		assertThat(videoUploadUrlIssuer.issuedRequests).containsExactly("7:TRAINING_VIDEO:video/mp4:10485760");
		assertThat(result.uploadUrl()).isEqualTo(UPLOAD_URL);
		assertThat(result.publicUrl()).isEqualTo(PUBLIC_URL);
		assertThat(result.objectKey()).isEqualTo(OBJECT_KEY);
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	@DisplayName("체험 횟수를 소진했으면 URL 발급 없이 예외를 던진다")
	void issueThrowsWithoutDelegationWhenTrialExhausted() {
		aiTrialFinder.usedCount = 3;

		assertThatThrownBy(() -> service.issue(7L, "video/mp4", 10485760L))
				.isInstanceOf(AiReportTrialExceededException.class);
		assertThat(videoUploadUrlIssuer.issuedRequests).isEmpty();
	}

	@Test
	@DisplayName("체험 현황은 요청한 사용자 기준으로 조회한다")
	void issueLooksUpTrialForRequestedUser() {
		service.issue(7L, "video/mp4", 10485760L);

		assertThat(aiTrialFinder.requestedUserIds).containsExactly(7L);
	}

	private static class StubAiTrialFinder implements AiTrialFinder {

		private final List<Long> requestedUserIds = new ArrayList<>();
		private int usedCount;

		@Override
		public AiTrial findTrial(Long userId) {
			requestedUserIds.add(userId);
			return new AiTrial(usedCount);
		}
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
