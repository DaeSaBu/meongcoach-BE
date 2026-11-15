package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class CurriculumProgressPersistenceTest {

	private static final Long USER_ID = 1L;

	private static final Long DOG_ID = 2L;

	private static final Long CURRICULUM_ID = 10L;

	private static final LocalDate TRAINED_DATE = LocalDate.of(2026, 11, 16);

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 훈련한_날짜와_상태를_저장하고_조회한다() {
		CurriculumProgress progress = register(DOG_ID, CURRICULUM_ID, TRAINED_DATE);
		entityManager.persistAndFlush(progress);
		Long progressId = progress.getId();
		entityManager.clear();

		CurriculumProgress found = entityManager.find(CurriculumProgress.class, progressId);

		assertThat(found.getTrainedDate()).isEqualTo(TRAINED_DATE);
		assertThat(found.getStatus()).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 판정을_기다리는_날을_성공으로_바꾸면_같은_행에_반영된다() {
		CurriculumProgress progress = register(DOG_ID, CURRICULUM_ID, TRAINED_DATE);
		entityManager.persistAndFlush(progress);
		Long progressId = progress.getId();
		entityManager.clear();

		CurriculumProgress found = entityManager.find(CurriculumProgress.class, progressId);
		found.succeed();
		entityManager.flush();
		entityManager.clear();

		CurriculumProgress updated = entityManager.find(CurriculumProgress.class, progressId);
		assertThat(updated.getStatus()).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 같은_강아지의_같은_커리큘럼은_하루에_한_행만_저장된다() {
		entityManager.persistAndFlush(register(DOG_ID, CURRICULUM_ID, TRAINED_DATE));
		CurriculumProgress duplicate = register(DOG_ID, CURRICULUM_ID, TRAINED_DATE);

		assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 날짜가_다르면_같은_커리큘럼도_따로_저장된다() {
		entityManager.persistAndFlush(register(DOG_ID, CURRICULUM_ID, TRAINED_DATE));
		CurriculumProgress nextDay = register(DOG_ID, CURRICULUM_ID, TRAINED_DATE.plusDays(1));

		entityManager.persistAndFlush(nextDay);

		assertThat(nextDay.getId()).isNotNull();
	}

	@Test
	void 강아지가_다르면_같은_날_같은_커리큘럼도_따로_저장된다() {
		entityManager.persistAndFlush(register(DOG_ID, CURRICULUM_ID, TRAINED_DATE));
		CurriculumProgress otherDog = register(3L, CURRICULUM_ID, TRAINED_DATE);

		entityManager.persistAndFlush(otherDog);

		assertThat(otherDog.getId()).isNotNull();
	}

	private CurriculumProgress register(Long dogId, Long curriculumId, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(USER_ID, dogId,
				curriculumId, trainedDate);
		return CurriculumProgress.register(command);
	}
}
