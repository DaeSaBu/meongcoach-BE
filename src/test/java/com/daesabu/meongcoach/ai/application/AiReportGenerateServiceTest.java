package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * 리포지토리까지 mock으로 두는 이유: "n번째 save만 실패" 같은 DB 장애 경로를 재현해야 하는데
 * 실제 H2로는 저장 실패를 주입할 수 없기 때문이다.
 */
@DisplayName("AI 리포트 생성 서비스")
class AiReportGenerateServiceTest {

	private static final Long USER_ID = 7L;
	private static final String OBJECT_KEY = "videos/training/7/key.mp4";
	private static final String DOWNLOAD_URL = "https://storage.test/download?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://videos.test/videos/training/7/key.mp4";
	private static final String ANALYZED_CONTENT = "{\"recommend\":[],\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\","
			+ "\"description\":\"분리불안 징후가 관찰됩니다.\"}],\"solution\":[]}";
	private static final String GENERATED_TITLE = "분리불안 징후 행동 분석";
	private static final IllegalStateException SAVE_FAILURE = new IllegalStateException("DB 저장 실패");

	private VideoDownloadUrlIssuer downloadUrlIssuer;
	private VideoAnalyzer videoAnalyzer;
	private ReportTitleGenerator reportTitleGenerator;
	private AiReportRepository aiReportRepository;
	private AiTrialFinder aiTrialFinder;
	private AiReportGenerateService service;

	// 서비스는 같은 AiReport 인스턴스를 전이시키며 save()를 여러 번 부르므로, ArgumentCaptor로는 최종 상태만 보인다.
	// 그래서 save 호출 시점의 상태를 순서대로 기록해 "PENDING이 먼저 저장됐다"를 검증한다
	private final List<AiReportStatus> savedStatuses = new ArrayList<>();
	private AiReport savedReport;
	private final Answer<AiReport> recordSave = invocation -> {
		savedReport = invocation.getArgument(0);
		savedStatuses.add(savedReport.getStatus());
		return savedReport;
	};

	@BeforeEach
	void setUp() {
		downloadUrlIssuer = mock(VideoDownloadUrlIssuer.class);
		videoAnalyzer = mock(VideoAnalyzer.class);
		reportTitleGenerator = mock(ReportTitleGenerator.class);
		aiReportRepository = mock(AiReportRepository.class);
		aiTrialFinder = mock(AiTrialFinder.class);
		service = new AiReportGenerateService(downloadUrlIssuer, videoAnalyzer, reportTitleGenerator,
				aiReportRepository, aiTrialFinder);

		// 기본은 성공 경로. 각 테스트는 깨뜨릴 협력자 하나만 덮어쓴다
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(false);
		when(downloadUrlIssuer.issue(OBJECT_KEY))
				.thenReturn(new VideoDownloadUrlResult(DOWNLOAD_URL, PUBLIC_URL, USER_ID, 3600L));
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(0));
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenReturn(ANALYZED_CONTENT);
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT)).thenReturn(GENERATED_TITLE);
		doAnswer(recordSave).when(aiReportRepository).save(any());
	}

	@Test
	@DisplayName("발급받은 presigned URL로 분석한 결과를 제목과 함께 COMPLETED 리포트로 저장한다")
	void generateSavesCompletedReportWithAnalyzedContent() {
		service.generate(OBJECT_KEY);

		// 분석기가 이 presigned URL로 영상을 직접 읽는다
		verify(videoAnalyzer).analyze(DOWNLOAD_URL);
		assertThat(savedReport.getUserId()).isEqualTo(USER_ID);
		assertThat(savedReport.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(savedReport.getTitle()).isEqualTo(GENERATED_TITLE);
		assertThat(savedReport.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedReport.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("분석을 시작하기 전에 PENDING 상태로 먼저 저장한다")
	void generateSavesPendingBeforeAnalysis() {
		service.generate(OBJECT_KEY);

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("제목 생성기에 분석 결과 JSON을 그대로 넘긴다")
	void generatePassesAnalyzedContentToTitleGenerator() {
		service.generate(OBJECT_KEY);

		verify(reportTitleGenerator).generateTitle(ANALYZED_CONTENT);
	}

	@Test
	@DisplayName("같은 영상의 리포트가 이미 있으면 URL 발급·분석·저장 없이 건너뛴다")
	void generateSkipsWhenReportAlreadyExists() {
		when(aiReportRepository.existsByVideoObjectKey(OBJECT_KEY)).thenReturn(true);

		service.generate(OBJECT_KEY);

		verifyNoInteractions(downloadUrlIssuer, videoAnalyzer);
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("객체 키 검증에 실패하면 row를 남기지 않고 예외를 그대로 던진다")
	void generatePropagatesInvalidObjectKeyWithoutSaving() {
		when(downloadUrlIssuer.issue(OBJECT_KEY)).thenThrow(new InvalidVideoObjectKeyException(OBJECT_KEY));

		// 소유자를 알 수 없어 상태를 기록할 row를 만들 수 없다. 컨슈머가 warn 로그로 버린다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	@DisplayName("영상 소유자가 체험 횟수를 소진했으면 분석 없이 FAILED_TRIAL_EXCEEDED로 기록한다")
	void generateRecordsTrialExceededWhenOwnerTrialExhausted() {
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(AiTrial.MAX_COUNT));

		service.generate(OBJECT_KEY);

		verify(videoAnalyzer, never()).analyze(any());
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	@DisplayName("분석이 실패하면 예외를 던지지 않고 FAILED_ANALYSIS로 기록한다")
	void generateRecordsAnalysisFailureWithoutThrowing() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));

		// 예외를 던지면 SQS가 같은 메시지를 무한히 재전달한다
		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_ANALYSIS);
		assertThat(savedReport.getContent()).isNull();
	}

	@Test
	@DisplayName("분석이 실패하면 제목 생성을 시도하지 않는다")
	void generateSkipsTitleGenerationWhenAnalysisFails() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));

		service.generate(OBJECT_KEY);

		verify(reportTitleGenerator, never()).generateTitle(any());
	}

	@Test
	@DisplayName("제목 생성이 실패하면 제목 없이 COMPLETED로 저장한다")
	void generateCompletesWithoutTitleWhenTitleGenerationFails() {
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT))
				.thenThrow(new ReportTitleGenerationFailedException("제목 생성 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedReport.getTitle()).isNull();
		assertThat(savedReport.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedReport.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("분석기가 선언되지 않은 예외를 던지면 FAILED_ANALYSIS가 아니라 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenAnalyzerThrowsUndeclaredException() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new IllegalStateException("어댑터 버그"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("제목 생성기가 선언되지 않은 예외를 던지면 제목 없이 완료하지 않고 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenTitleGeneratorThrowsUndeclaredException() {
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT)).thenThrow(new IllegalStateException("어댑터 버그"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("PENDING 기록 뒤 예상하지 못한 예외가 나면 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenTrialLookupThrows() {
		when(aiTrialFinder.findTrial(USER_ID)).thenThrow(new IllegalStateException("체험 조회 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("완료 저장이 실패하면 FAILED_UNEXPECTED로 기록한다")
	void generateRecordsUnexpectedFailureWhenCompletionSaveFails() {
		// 1번째(PENDING) 성공 → 2번째(COMPLETED) 실패 → 3번째(FAILED_UNEXPECTED) 성공
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiReportRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 상태 기록이 실패하면 FAILED_UNEXPECTED로 다시 기록을 시도한다")
	void generateRetriesAsUnexpectedFailureWhenFailureRecordingFails() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		// 1번째(PENDING) 성공 → 2번째(FAILED_ANALYSIS) 실패 → 3번째(FAILED_UNEXPECTED) 성공
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiReportRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	@DisplayName("실패 기록이 계속 실패하면 예외를 그대로 전파한다")
	void generatePropagatesWhenFailureRecordingKeepsFailing() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		// 1번째(PENDING) 성공 → 2번째부터 계속 실패 (마지막 스터빙이 반복된다)
		doAnswer(recordSave).doThrow(SAVE_FAILURE).when(aiReportRepository).save(any());

		// 더 할 수 있는 일이 없으므로 컨슈머의 최종 catch가 받아 error 로그 후 메시지를 버린다. row는 PENDING으로 남는다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(IllegalStateException.class);
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING);
	}
}
