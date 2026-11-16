package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainedLessonRecordCommand;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class TrainedLessonRepositoryTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long DOG_ID = 7L;

	private static final Long OTHER_DOG_ID = 8L;

	private static final Long LESSON_ID = 10L;

	private static final Long OTHER_LESSON_ID = 11L;

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 30);

	@Autowired
	private TrainedLessonRepository trainedLessonRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 그날_그_출처로_레슨을_훈련한_기록이_있는지_확인한다() {
		save(USER_ID, DOG_ID, LESSON_ID, FROM, TrainingSource.PLAN);
		flushAndClear();

		assertThat(exists(LESSON_ID, FROM, TrainingSource.PLAN)).isTrue();
		assertThat(exists(LESSON_ID, FROM, TrainingSource.LIBRARY)).isFalse();
		assertThat(exists(LESSON_ID, FROM.plusDays(1), TrainingSource.PLAN)).isFalse();
		assertThat(exists(OTHER_LESSON_ID, FROM, TrainingSource.PLAN)).isFalse();
	}

	@Test
	void 강아지가_시작일부터_종료일까지_훈련한_기록을_조회한다() {
		save(USER_ID, DOG_ID, LESSON_ID, FROM.minusDays(1), TrainingSource.PLAN);
		save(USER_ID, DOG_ID, LESSON_ID, FROM, TrainingSource.PLAN);
		save(USER_ID, DOG_ID, LESSON_ID, TO, TrainingSource.LIBRARY);
		save(USER_ID, DOG_ID, LESSON_ID, TO.plusDays(1), TrainingSource.PLAN);
		flushAndClear();

		List<TrainedLesson> trainedLessons = trainedLessonRepository
				.findAllByUserIdAndDogIdAndTrainedDateBetween(USER_ID, DOG_ID, FROM, TO);

		assertThat(trainedLessons).extracting(TrainedLesson::getTrainedDate).containsExactlyInAnyOrder(FROM, TO);
	}

	@Test
	void 다른_사용자나_다른_강아지가_훈련한_기록은_조회되지_않는다() {
		save(OTHER_USER_ID, DOG_ID, LESSON_ID, FROM, TrainingSource.PLAN);
		save(USER_ID, OTHER_DOG_ID, LESSON_ID, FROM, TrainingSource.PLAN);
		flushAndClear();

		List<TrainedLesson> trainedLessons = trainedLessonRepository
				.findAllByUserIdAndDogIdAndTrainedDateBetween(USER_ID, DOG_ID, FROM, TO);

		assertThat(trainedLessons).isEmpty();
	}

	@Test
	void 지정한_레슨들을_기준일_전에_훈련한_기록만_조회한다() {
		save(USER_ID, DOG_ID, LESSON_ID, FROM.minusDays(2), TrainingSource.PLAN);
		save(USER_ID, DOG_ID, LESSON_ID, FROM, TrainingSource.PLAN);
		save(USER_ID, DOG_ID, OTHER_LESSON_ID, FROM.minusDays(1), TrainingSource.PLAN);
		save(USER_ID, OTHER_DOG_ID, LESSON_ID, FROM.minusDays(3), TrainingSource.PLAN);
		flushAndClear();

		List<TrainedLesson> trainedLessons = trainedLessonRepository
				.findAllByUserIdAndDogIdAndLessonIdInAndTrainedDateBefore(USER_ID, DOG_ID, List.of(LESSON_ID), FROM);

		assertThat(trainedLessons).extracting(TrainedLesson::getTrainedDate).containsExactly(FROM.minusDays(2));
	}

	@Test
	void 강아지가_가장_먼저_훈련한_기록을_조회한다() {
		save(USER_ID, DOG_ID, LESSON_ID, TO, TrainingSource.PLAN);
		save(USER_ID, DOG_ID, OTHER_LESSON_ID, FROM, TrainingSource.LIBRARY);
		save(USER_ID, OTHER_DOG_ID, LESSON_ID, FROM.minusDays(10), TrainingSource.PLAN);
		flushAndClear();

		Optional<TrainedLesson> first = trainedLessonRepository
				.findFirstByUserIdAndDogIdOrderByTrainedDateAsc(USER_ID, DOG_ID);

		assertThat(first).get().extracting(TrainedLesson::getTrainedDate).isEqualTo(FROM);
	}

	@Test
	void 훈련한_기록이_없으면_가장_먼저_훈련한_기록이_없다() {
		Optional<TrainedLesson> first = trainedLessonRepository
				.findFirstByUserIdAndDogIdOrderByTrainedDateAsc(USER_ID, DOG_ID);

		assertThat(first).isEmpty();
	}

	private boolean exists(Long lessonId, LocalDate trainedDate, TrainingSource source) {
		return trainedLessonRepository.existsByUserIdAndDogIdAndLessonIdAndTrainedDateAndSource(USER_ID, DOG_ID,
				lessonId, trainedDate, source);
	}

	private void save(Long userId, Long dogId, Long lessonId, LocalDate trainedDate, TrainingSource source) {
		Instant completedAt = Instant.parse(trainedDate + "T03:00:00Z");
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(dogId, lessonId, source, completedAt);
		entityManager.persist(TrainedLesson.record(userId, command));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
