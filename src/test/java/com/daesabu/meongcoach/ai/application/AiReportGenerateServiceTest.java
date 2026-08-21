package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
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

	// save()에 넘어온 리포트는 같은 인스턴스라 ArgumentCaptor로는 최종 상태만 보이므로, 호출 시점의 상태를 따로 기록한다
	private List<AiReportStatus> savedStatuses;
	// n번째 save 호출에서 예외를 던지게 해 저장 실패 경로를 재현한다 (0이면 항상 성공)
	private int failingSaveCall;
	// n번째부터 모든 save 호출이 실패하게 해 DB 장애를 재현한다 (0이면 항상 성공)
	private int failingSaveCallFrom;
	private int saveCalls;

	@BeforeEach
	void setUp() {
		downloadUrlIssuer = new RecordingVideoDownloadUrlIssuer();
		videoAnalyzer = new RecordingVideoAnalyzer();
		reportTitleGenerator = new RecordingReportTitleGenerator();
		aiReportRepository = mock(AiReportRepository.class);
		aiTrialFinder = new StubAiTrialFinder();
		savedStatuses = new ArrayList<>();
		failingSaveCall = 0;
		failingSaveCallFrom = 0;
		saveCalls = 0;
		when(aiReportRepository.save(any())).thenAnswer(invocation -> {
			saveCalls++;
			if (saveCalls == failingSaveCall) {
				throw new IllegalStateException("DB 저장 실패");
			}
			if (failingSaveCallFrom != 0 && saveCalls >= failingSaveCallFrom) {
				throw new IllegalStateException("DB 장애");
			}
			AiReport report = invocation.getArgument(0);
			savedStatuses.add(report.getStatus());
			return report;
		});
		service = new AiReportGenerateService(downloadUrlIssuer, videoAnalyzer, reportTitleGenerator,
				aiReportRepository, aiTrialFinder);
	}

	@Test
	@DisplayName("발급받은 presigned URL로 분석한 결과를 제목과 함께 COMPLETED 리포트로 저장한다")
	void generateSavesCompletedReportWithAnalyzedContent() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).containsExactly(OBJECT_KEY);
		// 분석기가 이 presigned URL로 영상을 직접 읽는다
		assertThat(videoAnalyzer.videoUrls).containsExactly(DOWNLOAD_URL);
		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository, times(2)).save(captor.capture());
		AiReport saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo(7L);
		assertThat(saved.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(saved.getTitle()).isEqualTo(GENERATED_TITLE);
		assertThat(saved.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(saved.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("분석을 시작하기 전에 PENDING 상태로 먼저 저장한다")
	void generateSavesPendingBeforeAnalysis() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("제목 생성기에 분석 결과 JSON을 그대로 넘긴다")
	void generatePassesAnalyzedContentToTitleGenerator() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);

		service.generate(OBJECT_KEY);

		assertThat(reportTitleGenerator.reportContents).containsExactly(ANALYZED_CONTENT);
	}

	@Test
	@DisplayName("같은 영상의 리포트가 이미 있으면 URL 발급·분석·저장 없이 건너뛴다")
	void generateSkipsWhenReportAlreadyExists() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(true);

		service.generate(OBJECT_KEY);

		assertThat(downloadUrlIssuer.objectKeys).isEmpty();
		assertThat(videoAnalyzer.videoUrls).isEmpty();
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("객체 키 검증에 실패하면 row를 남기지 않고 예외를 그대로 던진다")
	void generatePropagatesInvalidObjectKeyWithoutSaving() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		downloadUrlIssuer.failure = new InvalidVideoObjectKeyException(OBJECT_KEY);

		// 소유자를 알 수 없어 상태를 기록할 row를 만들 수 없다. 컨슈머가 warn 로그로 버린다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("영상 소유자가 체험 횟수를 소진했으면 분석 없이 FAILED_TRIAL_EXCEEDED로 기록한다")
	void generateRecordsTrialExceededWhenOwnerTrialExhausted() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		aiTrialFinder.usedCount = 3;

		service.generate(OBJECT_KEY);

		assertThat(aiTrialFinder.requestedUserIds).containsExactly(7L);
		assertThat(videoAnalyzer.videoUrls).isEmpty();
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	@DisplayName("분석이 실패하면 예외를 던지지 않고 FAILED_ANALYSIS로 기록한다")
	void generateRecordsAnalysisFailureWithoutThrowing() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");

		// 예외를 던지면 SQS가 같은 메시지를 무한히 재전달한다
		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_ANALYSIS);
		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository, times(2)).save(captor.capture());
		assertThat(captor.getValue().getContent()).isNull();
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
	@DisplayName("제목 생성이 실패하면 제목 없이 COMPLETED로 저장한다")
	void generateCompletesWithoutTitleWhenTitleGenerationFails() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		reportTitleGenerator.failure = new IllegalStateException("제목 생성 실패");

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		ArgumentCaptor<AiReport> captor = ArgumentCaptor.forClass(AiReport.class);
		verify(aiReportRepository, times(2)).save(captor.capture());
		AiReport saved = captor.getValue();
		assertThat(saved.getTitle()).isNull();
		assertThat(saved.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(saved.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("PENDING 기록 뒤 예상하지 못한 예외가 나면 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenTrialLookupThrows() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		aiTrialFinder.failure = new IllegalStateException("체험 조회 실패");

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("완료 저장이 실패하면 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenCompletionSaveFails() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		failingSaveCall = 2;

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 상태 기록이 실패하면 FAILED_UNEXPECTED로 다시 기록을 시도한다")
	void generateRetriesAsUnexpectedFailureWhenFailureRecordingFails() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");
		failingSaveCall = 2;

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 기록이 계속 실패하면 예외를 그대로 전파한다")
	void generatePropagatesWhenFailureRecordingKeepsFailing() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		videoAnalyzer.failure = new IllegalStateException("분석 실패");
		failingSaveCallFrom = 2;

		// 더 할 수 있는 일이 없으므로 컨슈머의 최종 catch가 받아 error 로그 후 메시지를 버린다. row는 PENDING으로 남는다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(IllegalStateException.class);
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING);
	}

	private static class StubAiTrialFinder implements AiTrialFinder {

		private final List<Long> requestedUserIds = new ArrayList<>();
		private int usedCount;
		private RuntimeException failure;

		@Override
		public AiTrial findTrial(Long userId) {
			if (failure != null) {
				throw failure;
			}
			requestedUserIds.add(userId);
			return new AiTrial(usedCount);
		}
	}

	private static class RecordingVideoDownloadUrlIssuer implements VideoDownloadUrlIssuer {

		private final List<String> objectKeys = new ArrayList<>();
		private RuntimeException failure;

		@Override
		public VideoDownloadUrlResult issue(String objectKey) {
			if (failure != null) {
				throw failure;
			}
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
