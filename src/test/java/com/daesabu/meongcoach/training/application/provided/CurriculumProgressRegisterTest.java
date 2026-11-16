package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumProgressResultRequest;
import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressRegisterCommand;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressAlreadySucceededException;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.InvalidCurriculumProgressResultException;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurriculumProgressRegisterTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long CURRICULUM_ID = 10L;

	private static final LocalDate TRAINED_DATE = LocalDate.of(2026, 11, 16);

	private static final CurriculumProgressResultRequest SUCCEEDED = new CurriculumProgressResultRequest(
			"SUCCEEDED");

	private static final CurriculumProgressResultRequest FAILED = new CurriculumProgressResultRequest("FAILED");

	@Autowired
	private CurriculumProgressRegister curriculumProgressRegister;

	@Autowired
	private CurriculumProgressRepository curriculumProgressRepository;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	private Long dogId;

	@BeforeEach
	void setUp() {
		dogId = dogRepository.save(DogFixture.selected(USER_ID)).getId();
	}

	@Test
	void 판정을_기다리는_날을_성공으로_기록한다() {
		saveProgress(dogId, TRAINED_DATE);
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, SUCCEEDED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 판정을_기다리는_날을_실패로_기록한다() {
		saveProgress(dogId, TRAINED_DATE);
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, FAILED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.FAILED);
	}

	@Test
	void 실패한_날을_성공으로_바꾼다() {
		saveProgress(dogId, TRAINED_DATE).fail();
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, SUCCEEDED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 성공한_날에_성공을_다시_요청하면_오류_없이_그대로_둔다() {
		saveProgress(dogId, TRAINED_DATE).succeed();
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, SUCCEEDED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 실패한_날에_실패를_다시_요청하면_오류_없이_그대로_둔다() {
		saveProgress(dogId, TRAINED_DATE).fail();
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, FAILED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.FAILED);
	}

	@Test
	void 성공한_날을_실패로_바꾸려_하면_예외를_던지고_성공을_유지한다() {
		saveProgress(dogId, TRAINED_DATE).succeed();
		flushAndClear();

		assertThatThrownBy(
				() -> curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, FAILED))
				.isInstanceOf(CurriculumProgressAlreadySucceededException.class);

		flushAndClear();
		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@ParameterizedTest
	@ValueSource(strings = {"PENDING", "SUCCESS", "succeeded"})
	void 결과가_SUCCEEDED나_FAILED가_아니면_예외를_던진다(String result) {
		saveProgress(dogId, TRAINED_DATE);
		flushAndClear();
		CurriculumProgressResultRequest request = new CurriculumProgressResultRequest(result);

		assertThatThrownBy(
				() -> curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, request))
				.isInstanceOf(InvalidCurriculumProgressResultException.class);
	}

	@Test
	void 결과가_비어_있으면_검증_예외를_던진다() {
		CurriculumProgressResultRequest request = new CurriculumProgressResultRequest(" ");

		assertThatThrownBy(
				() -> curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 그날_그_커리큘럼을_훈련한_기록이_없으면_예외를_던진다() {
		saveProgress(dogId, TRAINED_DATE.minusDays(1));
		flushAndClear();

		assertThatThrownBy(
				() -> curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, SUCCEEDED))
				.isInstanceOf(CurriculumProgressNotFoundException.class);
	}

	@Test
	void 같은_사용자의_다른_강아지의_기록은_바꾸지_않는다() {
		Long otherDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		saveProgress(dogId, TRAINED_DATE);
		saveProgress(otherDogId, TRAINED_DATE);
		flushAndClear();

		curriculumProgressRegister.recordResult(USER_ID, dogId, CURRICULUM_ID, TRAINED_DATE, SUCCEEDED);
		flushAndClear();

		assertThat(findStatus(dogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
		assertThat(findStatus(otherDogId, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 다른_사용자의_강아지를_지정하면_예외를_던진다() {
		Long otherUserDogId = dogRepository.save(DogFixture.selected(OTHER_USER_ID)).getId();

		assertThatThrownBy(() -> curriculumProgressRegister.recordResult(USER_ID, otherUserDogId, CURRICULUM_ID,
				TRAINED_DATE, SUCCEEDED))
				.isInstanceOf(DogNotFoundException.class);
	}

	private CurriculumProgress saveProgress(Long dogId, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(dogId, CURRICULUM_ID,
				trainedDate);
		return curriculumProgressRepository.saveAndFlush(CurriculumProgress.register(USER_ID, command));
	}

	private CurriculumProgressStatus findStatus(Long dogId, LocalDate trainedDate) {
		return curriculumProgressRepository
				.findByUserIdAndDogIdAndCurriculumIdAndTrainedDate(USER_ID, dogId, CURRICULUM_ID, trainedDate)
				.orElseThrow()
				.getStatus();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
