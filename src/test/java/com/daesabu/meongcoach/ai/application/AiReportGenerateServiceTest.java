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
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
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
	private static final String ANALYZED_CONTENT = "{\"recommend\":[],\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\","
			+ "\"description\":\"분리불안 징후가 관찰됩니다.\"}],\"solution\":[]}";
	private static final String GENERATED_TITLE = "분리불안 징후 행동 분석";

	private RecordingVideoDownloadUrlIssuer downloadUrlIssuer;
	private RecordingVideoAnalyzer videoAnalyzer;
	private RecordingReportTitleGenerator reportTitleGenerator;
	private AiReportRepository aiReportRepository;
	private StubAiTrialFinder aiTrialFinder;
	private AiReportGenerateService service;

	@BeforeEach
	void setUp() {
		downloadUrlIssuer = new RecordingVideoDownloadUrlIssuer();
		videoAnalyzer = new RecordingVideoAnalyzer();
		reportTitleGenerator = new RecordingReportTitleGenerator();
		aiReportRepository = mock(AiReportRepository.class);
		aiTrialFinder = new StubAiTrialFinder();
		service = new AiReportGenerateService(downloadUrlIssuer, videoAnalyzer, reportTitleGenerator,
				aiReportRepository, aiTrialFinder);
	}

	@Test
	@DisplayName("발급받은 presigned URL로 분석한 결과를 제목과 함께 리포트로 저장한다")
	void generateSavesReportWithAnalyzedContent() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).containsExactly(OBJECT_KEY);
		// 분석기가 이 presigned URL로 영상을 직접 읽는다
		assertThat(videoAnalyzer.videoUrls).containsExactly(DOWNLOAD_URL);
		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository).save(captor.capture());
		AiReport saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo(7L);
		assertThat(saved.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(saved.getTitle()).isEqualTo(GENERATED_TITLE);
		assertThat(saved.getContent()).isEqualTo(ANALYZED_CONTENT);
	}

	@Test
	@DisplayName("제목 생성기에 분석 결과 JSON을 그대로 넘긴다")
	void generatePassesAnalyzedContentToTitleGenerator() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(reportTitleGenerator.reportContents).containsExactly(ANALYZED_CONTENT);
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
		aiTrialFinder.usedCount = 3;

		service.generate(OBJECT_KEY);

		assertThat(aiTrialFinder.requestedUserIds).containsExactly(7L);
		assertThat(videoAnalyzer.videoUrls).isEmpty();
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

	@Test
	@DisplayName("분석이 실패하면 제목 생성을 시도하지 않는다")
	void generateSkipsTitleGenerationWhenAnalysisFails() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");

		service.generate(OBJECT_KEY);

		assertThat(reportTitleGenerator.reportContents).isEmpty();
	}

	@Test
	@DisplayName("제목 생성이 실패하면 제목 없이 리포트를 저장한다")
	void generateSavesReportWithoutTitleWhenTitleGenerationFails() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		reportTitleGenerator.failure = new IllegalStateException("제목 생성 실패");

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository).save(captor.capture());
		AiReport saved = captor.getValue();
		assertThat(saved.getTitle()).isNull();
		assertThat(saved.getContent()).isEqualTo(ANALYZED_CONTENT);
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
			return ANALYZED_CONTENT;
		}
	}

	private static class RecordingReportTitleGenerator implements ReportTitleGenerator {

		private final List<String> reportContents = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public String generateTitle(String reportContentJson) {
			if (failure != null) {
				throw failure;
			}
			reportContents.add(reportContentJson);
			return GENERATED_TITLE;
		}
	}
}
