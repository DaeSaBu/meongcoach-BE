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
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiVideo;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.media.application.provided.MediaStorageUriResolver;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class AiReportGenerateServiceTest {

	private static final Long USER_ID = 7L;
	private static final String OBJECT_KEY = "videos/training/7/key.mp4";
	private static final String VIDEO_URI = "gs://test-ai-video-bucket/videos/training/7/key.mp4";
	private static final String ANALYZED_CONTENT = "{\"recommend\":[],\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\","
			+ "\"description\":\"분리불안 징후가 관찰됩니다.\"}],\"solution\":[]}";
	private static final String GENERATED_TITLE = "분리불안 징후 행동 분석";
	private static final IllegalStateException SAVE_FAILURE = new IllegalStateException("DB 저장 실패");

	private MediaStorageUriResolver storageUriResolver;
	private VideoAnalyzer videoAnalyzer;
	private AiAnalysisRepository aiAnalysisRepository;
	private AiTrialFinder aiTrialFinder;
	private AiReportGenerateService service;

	private AiAnalysis issuedAnalysis;

	private final List<AiAnalysisStatus> savedStatuses = new ArrayList<>();
	private AiAnalysis savedAnalysis;
	private final Answer<AiAnalysis> recordSave = invocation -> {
		savedAnalysis = invocation.getArgument(0);
		savedStatuses.add(savedAnalysis.getStatus());
		return savedAnalysis;
	};

	@BeforeEach
	void setUp() {
		storageUriResolver = mock(MediaStorageUriResolver.class);
		videoAnalyzer = mock(VideoAnalyzer.class);
		aiAnalysisRepository = mock(AiAnalysisRepository.class);
		aiTrialFinder = mock(AiTrialFinder.class);
		service = new AiReportGenerateService(storageUriResolver, videoAnalyzer, aiAnalysisRepository, aiTrialFinder);

		issuedAnalysis = AiAnalysis.uploading(
				new AiAnalysisUploadCommand(USER_ID, OBJECT_KEY, Instant.now().plusSeconds(900)));
		when(aiAnalysisRepository.findByVideoObjectKey(OBJECT_KEY)).thenReturn(Optional.of(issuedAnalysis));
		when(storageUriResolver.resolve(AiVideo.SCOPE, OBJECT_KEY)).thenReturn(VIDEO_URI);
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(0));
		when(videoAnalyzer.analyze(VIDEO_URI)).thenReturn(new VideoAnalysisResult(GENERATED_TITLE, ANALYZED_CONTENT));
		doAnswer(recordSave).when(aiAnalysisRepository).save(any());
	}

	@Test
	void 발급받은_presigned_URL로_분석한_결과를_제목과_함께_발급_시_만든_row에_COMPLETED로_저장한다() {
		service.generate(OBJECT_KEY);

		verify(videoAnalyzer).analyze(VIDEO_URI);
		assertThat(savedAnalysis).isSameAs(issuedAnalysis);
		assertThat(savedAnalysis.getUserId()).isEqualTo(USER_ID);
		assertThat(savedAnalysis.getVideoObjectKey()).isEqualTo(OBJECT_KEY);
		assertThat(savedAnalysis.getTitle()).isEqualTo(GENERATED_TITLE);
		assertThat(savedAnalysis.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedAnalysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 분석을_시작하기_전에_UPLOADING_row를_PENDING으로_먼저_전이해_저장한다() {
		service.generate(OBJECT_KEY);

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 같은_영상의_리포트가_이미_분석을_시작했으면_URL_발급_분석_저장_없이_건너뛴다() {
		issuedAnalysis.startAnalysis();

		service.generate(OBJECT_KEY);

		verifyNoInteractions(storageUriResolver, videoAnalyzer);
		verify(aiAnalysisRepository, never()).save(any());
	}

	@Test
	void 업로드_URL_발급_기록이_없는_영상이면_URL_발급_분석_저장_없이_건너뛴다() {
		when(aiAnalysisRepository.findByVideoObjectKey(OBJECT_KEY)).thenReturn(Optional.empty());

		service.generate(OBJECT_KEY);

		verifyNoInteractions(storageUriResolver, videoAnalyzer);
		verify(aiAnalysisRepository, never()).save(any());
	}

	@Test
	void 객체_키_검증에_실패하면_PENDING으로_전이하지_않고_예외를_그대로_던진다() {
		when(storageUriResolver.resolve(AiVideo.SCOPE, OBJECT_KEY)).thenThrow(new InvalidMediaObjectKeyException(OBJECT_KEY));

		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(InvalidMediaObjectKeyException.class);
		verify(aiAnalysisRepository, never()).save(any());
		assertThat(issuedAnalysis.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
	}

	@Test
	void 영상_소유자가_체험_횟수를_소진했으면_분석_없이_FAILED_TRIAL_EXCEEDED로_기록한다() {
		when(aiTrialFinder.findTrial(USER_ID)).thenReturn(new AiTrial(AiTrial.MAX_COUNT));

		service.generate(OBJECT_KEY);

		verify(videoAnalyzer, never()).analyze(any());
		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_TRIAL_EXCEEDED);
	}

	@Test
	void 분석이_실패하면_예외를_던지지_않고_FAILED_ANALYSIS로_기록한다() {
		when(videoAnalyzer.analyze(VIDEO_URI)).thenThrow(new VideoAnalysisFailedException("분석 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_ANALYSIS);
		assertThat(savedAnalysis.getContent()).isNull();
	}

	@Test
	void 분석_결과에_제목이_없으면_제목_없이_COMPLETED로_저장한다() {
		when(videoAnalyzer.analyze(VIDEO_URI)).thenReturn(new VideoAnalysisResult(null, ANALYZED_CONTENT));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedAnalysis.getTitle()).isNull();
		assertThat(savedAnalysis.getContent()).isEqualTo(ANALYZED_CONTENT);
		assertThat(savedAnalysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void 분석기가_선언되지_않은_예외를_던지면_FAILED_ANALYSIS가_아니라_FAILED_UNEXPECTED로_기록한다() {
		when(videoAnalyzer.analyze(VIDEO_URI)).thenThrow(new IllegalStateException("어댑터 버그"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_UNEXPECTED);
	}

	@Test
	void PENDING_기록_뒤_예상하지_못한_예외가_나면_FAILED_UNEXPECTED로_기록한다() {
		when(aiTrialFinder.findTrial(USER_ID)).thenThrow(new IllegalStateException("체험 조회 실패"));

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 완료_저장이_실패하면_FAILED_UNEXPECTED로_기록한다() {
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiAnalysisRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_UNEXPECTED);
		assertThat(savedAnalysis.getReport()).isNull();
	}

	@Test
	void 실패_상태_기록이_실패하면_FAILED_UNEXPECTED로_다시_기록을_시도한다() {
		when(videoAnalyzer.analyze(VIDEO_URI)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		doAnswer(recordSave).doThrow(SAVE_FAILURE).doAnswer(recordSave).when(aiAnalysisRepository).save(any());

		assertThatCode(() -> service.generate(OBJECT_KEY)).doesNotThrowAnyException();

		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING, AiAnalysisStatus.FAILED_UNEXPECTED);
	}

	@Test
	void 실패_기록이_계속_실패하면_예외를_그대로_전파한다() {
		when(videoAnalyzer.analyze(VIDEO_URI)).thenThrow(new VideoAnalysisFailedException("분석 실패"));
		doAnswer(recordSave).doThrow(SAVE_FAILURE).when(aiAnalysisRepository).save(any());

		assertThatThrownBy(() -> service.generate(OBJECT_KEY))
				.isInstanceOf(IllegalStateException.class);
		assertThat(savedStatuses).containsExactly(AiAnalysisStatus.PENDING);
	}
}
