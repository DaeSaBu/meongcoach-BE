package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressRegisterCommand;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class CurriculumProgressRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long DOG_ID = 7L;

	private static final Long OTHER_DOG_ID = 8L;

	private static final Long CURRICULUM_ID = 10L;

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 30);

	@Autowired
	private CurriculumProgressRepository curriculumProgressRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 강아지가_그날_커리큘럼을_훈련한_기록이_있는지_확인한다() {
		save(USER_ID, DOG_ID, CURRICULUM_ID, FROM);
		flushAndClear();

		assertThat(exists(DOG_ID, CURRICULUM_ID, FROM)).isTrue();
		assertThat(exists(DOG_ID, CURRICULUM_ID, FROM.plusDays(1))).isFalse();
		assertThat(exists(DOG_ID, 11L, FROM)).isFalse();
		assertThat(exists(OTHER_DOG_ID, CURRICULUM_ID, FROM)).isFalse();
	}

	@Test
	void 강아지가_그날_훈련한_커리큘럼의_기록을_조회한다() {
		save(USER_ID, DOG_ID, CURRICULUM_ID, FROM);
		save(USER_ID, OTHER_DOG_ID, CURRICULUM_ID, FROM);
		flushAndClear();

		Optional<CurriculumProgress> found = curriculumProgressRepository
				.findByUserIdAndDogIdAndCurriculumIdAndTrainedDate(USER_ID, DOG_ID, CURRICULUM_ID, FROM);

		assertThat(found).get().extracting(CurriculumProgress::getDogId).isEqualTo(DOG_ID);
	}

	@Test
	void 잠금_조회도_강아지가_그날_훈련한_커리큘럼의_기록만_찾는다() {
		save(USER_ID, DOG_ID, CURRICULUM_ID, FROM);
		save(OTHER_USER_ID, DOG_ID, CURRICULUM_ID, FROM.plusDays(1));
		flushAndClear();

		Optional<CurriculumProgress> found = curriculumProgressRepository
				.findLockedByUserIdAndDogIdAndCurriculumIdAndTrainedDate(USER_ID, DOG_ID, CURRICULUM_ID, FROM);
		Optional<CurriculumProgress> otherDay = curriculumProgressRepository
				.findLockedByUserIdAndDogIdAndCurriculumIdAndTrainedDate(USER_ID, DOG_ID, CURRICULUM_ID,
						FROM.plusDays(1));

		assertThat(found).isPresent();
		assertThat(otherDay).isEmpty();
	}

	@Test
	void 강아지가_시작일부터_종료일까지_훈련한_커리큘럼의_기록을_조회한다() {
		save(USER_ID, DOG_ID, CURRICULUM_ID, FROM.minusDays(1));
		save(USER_ID, DOG_ID, CURRICULUM_ID, FROM);
		save(USER_ID, DOG_ID, CURRICULUM_ID, TO);
		save(USER_ID, DOG_ID, CURRICULUM_ID, TO.plusDays(1));
		save(USER_ID, OTHER_DOG_ID, CURRICULUM_ID, FROM.plusDays(1));
		flushAndClear();

		List<CurriculumProgress> progresses = curriculumProgressRepository
				.findAllByUserIdAndDogIdAndTrainedDateBetween(USER_ID, DOG_ID, FROM, TO);

		assertThat(progresses).extracting(CurriculumProgress::getTrainedDate).containsExactlyInAnyOrder(FROM, TO);
	}

	private boolean exists(Long dogId, Long curriculumId, LocalDate trainedDate) {
		return curriculumProgressRepository.existsByUserIdAndDogIdAndCurriculumIdAndTrainedDate(USER_ID, dogId,
				curriculumId, trainedDate);
	}

	private void save(Long userId, Long dogId, Long curriculumId, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(dogId, curriculumId,
				trainedDate);
		entityManager.persist(CurriculumProgress.register(userId, command));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
