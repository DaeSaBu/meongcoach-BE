package com.daesabu.meongcoach.analysis.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.analysis.application.provided.dto.AiAnalysisRegisterRequest;
import com.daesabu.meongcoach.analysis.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiAnalysis;
import com.daesabu.meongcoach.analysis.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.analysis.domain.AiTrialFixture;
import com.daesabu.meongcoach.analysis.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.analysis.domain.exception.AiVideoDurationOutOfRangeException;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AiAnalysisRegisterTest {

	private static final Long USER_ID = 7L;
	private static final String GCS_BASE_URL = "https://storage.googleapis.com/test-ai-video-bucket/";
	private static final long EXPIRES_IN_SECONDS = 900L;
	private static final String CONTEXT = "퇴근하고 집에 오면 매번 이렇게 짖어요";
	private static final AiAnalysisRegisterRequest REQUEST =
			new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, CONTEXT);

	@Autowired
	private AiAnalysisRegister aiAnalysisRegister;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Autowired
	private DogRepository dogRepository;

	private Long dogId;

	@BeforeEach
	void setUp() {
		dogId = dogRepository.save(DogFixture.selected(USER_ID)).getId();
	}

	@Test
	void 지정한_강아지로_분석_건을_저장한다() {
		Long otherDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, otherDogId, REQUEST);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getDogId()).isEqualTo(otherDogId);
	}

	@Test
	void dogId를_생략하면_선택된_강아지로_분석_건을_저장한다() {
		dogRepository.save(DogFixture.unselected(USER_ID));

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, null, REQUEST);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getDogId()).isEqualTo(dogId);
	}

	@Test
	void 다른_사용자의_강아지를_지정하면_예외를_던지고_분석_건을_만들지_않는다() {
		Long otherUserDogId = dogRepository.save(DogFixture.selected(8L)).getId();

		assertThatThrownBy(() -> aiAnalysisRegister.register(USER_ID, otherUserDogId, REQUEST))
				.isInstanceOf(DogNotFoundException.class);
		assertThat(aiAnalysisRepository.count()).isZero();
	}

	@Test
	void 맥락을_담은_UPLOADING_분석_건을_저장하고_그_ID와_업로드_URL을_돌려준다() {
		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, dogId, REQUEST);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getUserId()).isEqualTo(USER_ID);
		assertThat(saved.getContext()).isEqualTo(CONTEXT);
		assertThat(saved.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADING);
		assertThat(saved.getVideoObjectKey()).isEqualTo(result.objectKey());
		assertThat(result.objectKey()).matches("videos/training/7/[0-9a-f-]{36}\\.mp4");
		assertThat(result.uploadUrl())
				.startsWith(GCS_BASE_URL + result.objectKey())
				.contains("X-Goog-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(result.expiresInSeconds()).isEqualTo(EXPIRES_IN_SECONDS);
	}

	@Test
	void 업로드_만료_시각은_요청_시각에_URL_유효_시간을_더한_값이다() {
		Instant before = Instant.now();

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, dogId, REQUEST);

		Instant after = Instant.now();
		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getUploadExpiresAt())
				.isBetween(before.plusSeconds(EXPIRES_IN_SECONDS), after.plusSeconds(EXPIRES_IN_SECONDS));
	}

	@Test
	void 맥락_없이도_분석_건을_만든다() {
		AiAnalysisRegisterRequest request = new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, null);

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, dogId, request);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getContext()).isNull();
	}

	@Test
	void 맥락은_500자까지_받는다() {
		AiAnalysisRegisterRequest request =
				new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, "가".repeat(AiAnalysis.CONTEXT_MAX_LENGTH));

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, dogId, request);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getContext()).hasSize(AiAnalysis.CONTEXT_MAX_LENGTH);
	}

	@Test
	void 맥락_길이는_앞뒤_공백을_지운_뒤_센다() {
		String context = "  " + "가".repeat(AiAnalysis.CONTEXT_MAX_LENGTH) + "\n";
		AiAnalysisRegisterRequest request = new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, context);

		AiAnalysisRegisterResult result = aiAnalysisRegister.register(USER_ID, dogId, request);

		AiAnalysis saved = aiAnalysisRepository.findById(result.analysisId()).orElseThrow();
		assertThat(saved.getContext()).isEqualTo("가".repeat(AiAnalysis.CONTEXT_MAX_LENGTH));
	}

	@Test
	void 맥락이_500자를_넘으면_검증에_실패한다() {
		AiAnalysisRegisterRequest request = new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L,
				"가".repeat(AiAnalysis.CONTEXT_MAX_LENGTH + 1));

		assertThatThrownBy(() -> aiAnalysisRegister.register(USER_ID, dogId, request))
				.isInstanceOf(ConstraintViolationException.class);
		assertThat(aiAnalysisRepository.findAllByUserIdAndDogIdOrderByCreatedAtDescIdDesc(USER_ID, dogId)).isEmpty();
	}

	@Test
	void 체험_횟수를_소진했으면_분석_건_저장_없이_예외를_던진다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(USER_ID, AiTrial.DEFAULT_GRANTED_COUNT));

		assertThatThrownBy(() -> aiAnalysisRegister.register(USER_ID, dogId, REQUEST))
				.isInstanceOf(AiReportTrialExceededException.class);
		assertThat(aiAnalysisRepository.findAllByUserIdAndDogIdOrderByCreatedAtDescIdDesc(USER_ID, dogId)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(longs = {9L, 301L})
	void 영상_길이가_허용_범위를_벗어나면_분석_건_저장_없이_예외를_던진다(long durationSeconds) {
		AiAnalysisRegisterRequest request =
				new AiAnalysisRegisterRequest("video/mp4", 10485760L, durationSeconds, CONTEXT);

		assertThatThrownBy(() -> aiAnalysisRegister.register(USER_ID, dogId, request))
				.isInstanceOf(AiVideoDurationOutOfRangeException.class);
		assertThat(aiAnalysisRepository.findAllByUserIdAndDogIdOrderByCreatedAtDescIdDesc(USER_ID, dogId)).isEmpty();
	}
}
