package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("AI 리포트 생성 서비스")
class AiReportGenerateServiceTest {

	private static final String OBJECT_KEY = "videos/training/7/key.mp4";
	private static final String DOWNLOAD_URL = "https://storage.test/download?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://videos.test/videos/training/7/key.mp4";
	private static final String S3_URI = "s3://test-video-bucket/videos/training/7/key.mp4";

	private RecordingVideoDownloadUrlIssuer downloadUrlIssuer;
	private RecordingVideoAnalyzer videoAnalyzer;
	private AiReportRepository aiReportRepository;
	private StubAiTrialFinder aiTrialFinder;
	private AiReportGenerateService service;

	@BeforeEach
	void setUp() {
		downloadUrlIssuer = new RecordingVideoDownloadUrlIssuer();
		videoAnalyzer = new RecordingVideoAnalyzer();
		aiReportRepository = mock(AiReportRepository.class);
		aiTrialFinder = new StubAiTrialFinder();
		service = new AiReportGenerateService(downloadUrlIssuer, videoAnalyzer, aiReportRepository, aiTrialFinder);
	}

	@Test
	@DisplayName("발급받은 영상 위치로 분석한 결과를 리포트로 저장한다")
	void generateSavesReportWithAnalyzedContent() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).containsExactly(OBJECT_KEY);
		// Bedrock이 버킷에서 영상을 직접 읽으므로 presigned URL이 아니라 s3 URI를 넘긴다
		assertThat(videoAnalyzer.videoS3Uris).containsExactly(S3_URI);
		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository).save(captor.capture());
		AiReport saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo(7L);
		assertThat(saved.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(saved.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("같은 영상의 리포트가 이미 있으면 URL 발급·분석 없이 건너뛴다")
	void generateSkipsWhenReportAlreadyExists() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(true);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).isEmpty();
		assertThat(videoAnalyzer.videoS3Uris).isEmpty();
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("영상 소유자가 체험 횟수를 소진했으면 분석 없이 건너뛴다")
	void generateSkipsWhenOwnerTrialExhausted() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		aiTrialFinder.usedCount = 3;

		service.generate(OBJECT_KEY);

		assertThat(aiTrialFinder.requestedUserIds).containsExactly(7L);
		assertThat(videoAnalyzer.videoS3Uris).isEmpty();
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("분석이 실패해도 예외를 던지지 않고 저장만 건너뛴다")
	void generateSwallowsAnalysisFailureWithoutSaving() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");

		// 예외를 던지면 SQS가 같은 메시지를 무한히 재전달한다
		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();
		verify(aiReportRepository, never()).save(any());
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

	private static class RecordingVideoDownloadUrlIssuer implements VideoDownloadUrlIssuer {

		private final List<String> objectKeys = new ArrayList<>();

		@Override
		public VideoDownloadUrlResult issue(String objectKey) {
			objectKeys.add(objectKey);
			return new VideoDownloadUrlResult(DOWNLOAD_URL, PUBLIC_URL, S3_URI, 7L, 3600L);
		}
	}

	private static class RecordingVideoAnalyzer implements VideoAnalyzer {

		private final List<String> videoS3Uris = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public String analyze(String videoS3Uri) {
			if (failure != null) {
				throw failure;
			}
			videoS3Uris.add(videoS3Uri);
			return "분리불안 징후가 관찰됩니다.";
		}
	}
}
