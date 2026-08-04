package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
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

	private RecordingVideoDownloadUrlIssuer downloadUrlIssuer;
	private RecordingVideoAnalyzer videoAnalyzer;
	private AiReportRepository aiReportRepository;
	private AiReportGenerateService service;

	@BeforeEach
	void setUp() {
		downloadUrlIssuer = new RecordingVideoDownloadUrlIssuer();
		videoAnalyzer = new RecordingVideoAnalyzer();
		aiReportRepository = mock(AiReportRepository.class);
		service = new AiReportGenerateService(downloadUrlIssuer, videoAnalyzer, aiReportRepository);
	}

	@Test
	@DisplayName("발급받은 다운로드 URL로 분석한 결과를 리포트로 저장한다")
	void generateSavesReportWithAnalyzedContent() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).containsExactly(OBJECT_KEY);
		assertThat(videoAnalyzer.videoUrls).containsExactly(DOWNLOAD_URL);
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
		assertThat(videoAnalyzer.videoUrls).isEmpty();
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("영상 소유자가 체험 횟수를 소진했으면 분석 없이 건너뛴다")
	void generateSkipsWhenOwnerTrialExhausted() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		when(aiReportRepository.countByUserId(7L)).thenReturn(3L);

		service.generate(OBJECT_KEY);

		assertThat(videoAnalyzer.videoUrls).isEmpty();
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("분석이 실패하면 예외를 전파하고 저장하지 않는다")
	void generatePropagatesAnalysisFailureWithoutSaving() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");

		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(IllegalStateException.class);
		verify(aiReportRepository, never()).save(any());
	}

	private static class RecordingVideoDownloadUrlIssuer implements VideoDownloadUrlIssuer {

		private final List<String> objectKeys = new ArrayList<>();

		@Override
		public VideoDownloadUrlResult issue(String objectKey) {
			objectKeys.add(objectKey);
			return new VideoDownloadUrlResult(DOWNLOAD_URL, PUBLIC_URL, 7L, 3600L);
		}
	}

	private static class RecordingVideoAnalyzer implements VideoAnalyzer {

		private final List<String> videoUrls = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public String analyze(String videoUrl) {
			if (failure != null) {
				throw failure;
			}
			videoUrls.add(videoUrl);
			return "분리불안 징후가 관찰됩니다.";
		}
	}
}
