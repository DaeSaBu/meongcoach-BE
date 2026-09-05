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
import com.daesabu.meongcoach.ai.domain.AiReportUploadCommand;
import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * 리포지토리까지 mock으로 두는 이유: "n번째 save만 실패" 같은 DB 장애 경로를 재현해야 하는데
 * 실제 DB로는 저장 실패를 주입할 수 없기 때문이다.
 */
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

	// 업로드 URL 발급 시 저장된 UPLOADING row. 서비스는 이 row를 찾아 전이시킨다
	private AiReport issuedReport;

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
		issuedReport = AiReport.uploading(
				new AiReportUploadCommand(USER_ID, OBJECT_KEY, LocalDateTime.now().plusMinutes(15)));
		when(aiReportRepository.findByVideoObjectKey(OBJECT_KEY)).thenReturn(Optional.of(issuedReport));
		when(downloadUrlIssuer.issue(OBJECT_KEY))
				.thenReturn(new VideoDownloadUrlResult(DOWNLOAD_URL, PUBLIC_URL, USER_ID, 3600L));
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(0));
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenReturn(ANALYZED_CONTENT);
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT)).thenReturn(GENERATED_TITLE);
		doAnswer(recordSave).when(aiReportRepository).save(any());
	}

	@Test
	void 발급받은_presigned_URL로_분석한_결과를_제목과_함께_발급_시_만든_row에_COMPLETED로_저장한다() {
		service.generate(OBJECT_KEY);

		// 분석기가 이 presigned URL로 영상을 직접 읽는다
		verify(videoAnalyzer).analyze(DOWNLOAD_URL);
		assertThat(savedReport).isSameAs(issuedReport);
		assertThat(savedReport.getUserId()).isEqualTo(USER_ID);
		assertThat(savedReport.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(savedReport.getTitle()).isEqualTo(GENERATED_TITLE);
		assertThat(savedReport.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedReport.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	void 분석을_시작하기_전에_UPLOADING_row를_PENDING으로_먼저_전이해_저장한다() {
		service.generate(OBJECT_KEY);

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.COMPLETED);
	}

	@Test
	void 제목_생성기에_분석_결과_JSON을_그대로_넘긴다() {
		service.generate(OBJECT_KEY);

		verify(reportTitleGenerator).generateTitle(ANALYZED_CONTENT);
	}

	@Test
	void 같은_영상의_리포트가_이미_분석을_시작했으면_URL_발급_분석_저장_없이_건너뛴다() {
		issuedReport.startAnalysis();

		service.generate(OBJECT_KEY);

		verifyNoInteractions(downloadUrlIssuer, videoAnalyzer);
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	void 업로드_URL_발급_기록이_없는_영상이면_URL_발급_분석_저장_없이_건너뛴다() {
		when(aiReportRepository.findByVideoObjectKey(OBJECT_KEY)).thenReturn(Optional.empty());

		service.generate(OBJECT_KEY);

		verifyNoInteractions(downloadUrlIssuer, videoAnalyzer);
		verify(aiReportRepository, never()).save(any());
	}

	@Test
	void 객체_키_검증에_실패하면_PENDING으로_전이하지_않고_예외를_그대로_던진다() {
		when(downloadUrlIssuer.issue(OBJECT_KEY)).thenThrow(new InvalidVideoObjectKeyException(OBJECT_KEY));

		// row는 UPLOADING으로 남아 만료 뒤 FAILED_UPLOAD로 조회된다. 컨슈머가 warn 로그로 버린다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		verify(aiReportRepository, never()).save(any());
		assertThat(issuedReport.getStatus()).isEqualTo(AiReportStatus.UPLOADING);
	}

	@Test
	void 영상_소유자가_체험_횟수를_소진했으면_분석_없이_FAILED_TRIAL_EXCEEDED로_기록한다() {
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(AiTrial.MAX_COUNT));

		service.generate(OBJECT_KEY);

		verify(videoAnalyzer, never()).analyze(any());
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	void 분석이_실패하면_예외를_던지지_않고_FAILED_ANALYSIS로_기록한다() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));

		// 예외를 던지면 SQS가 같은 메시지를 무한히 재전달한다
		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_ANALYSIS);
		assertThat(savedReport.getContent()).isNull();
	}

	@Test
	void 분석이_실패하면_제목_생성을_시도하지_않는다() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));

		service.generate(OBJECT_KEY);

		verify(reportTitleGenerator, never()).generateTitle(any());
	}

	@Test
	void 제목_생성이_실패하면_제목_없이_COMPLETED로_저장한다() {
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT))
				.thenThrow(new ReportTitleGenerationFailedException("제목 생성 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedReport.getTitle()).isNull();
		assertThat(savedReport.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedReport.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	void 분석기가_선언되지_않은_예외를_던지면_FAILED_ANALYSIS가_아니라_FAILED_UNEXPECTED로_기록한다() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new IllegalStateException("어댑터 버그"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 제목_생성기가_선언되지_않은_예외를_던지면_제목_없이_완료하지_않고_FAILED_UNEXPECTED로_기록한다() {
		when(reportTitleGenerator.generateTitle(ANALYZED_CONTENT)).thenThrow(new IllegalStateException("어댑터 버그"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void PENDING_기록_뒤_예상하지_못한_예외가_나면_FAILED_UNEXPECTED로_기록한다() {
		when(aiTrialFinder.findTrial(USER_ID)).thenThrow(new IllegalStateException("체험 조회 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 완료_저장이_실패하면_FAILED_UNEXPECTED로_기록한다() {
		// 1번째(PENDING) 성공 → 2번째(COMPLETED) 실패 → 3번째(FAILED_UNEXPECTED) 성공
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiReportRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 실패_상태_기록이_실패하면_FAILED_UNEXPECTED로_다시_기록을_시도한다() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		// 1번째(PENDING) 성공 → 2번째(FAILED_ANALYSIS) 실패 → 3번째(FAILED_UNEXPECTED) 성공
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiReportRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING, AiReportStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 실패_기록이_계속_실패하면_예외를_그대로_전파한다() {
		when(videoAnalyzer.analyze(DOWNLOAD_URL)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		// 1번째(PENDING) 성공 → 2번째부터 계속 실패 (마지막 스터빙이 반복된다)
		doAnswer(recordSave).doThrow(SAVE_FAILURE).when(aiReportRepository).save(any());

		// 더 할 수 있는 일이 없으므로 컨슈머의 최종 catch가 받아 error 로그 후 메시지를 버린다. row는 PENDING으로 남는다
		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(IllegalStateException.class);
		assertThat(savedStatuses).containsExactly(AiReportStatus.PENDING);
	}
}
