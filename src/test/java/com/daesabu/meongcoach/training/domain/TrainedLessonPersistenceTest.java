package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class TrainedLessonPersistenceTest {

	private static final Long USER_ID = 1L;

	private static final Long DOG_ID = 2L;

	private static final Long LESSON_ID = 10L;

	private static final Instant COMPLETED_AT = Instant.parse("2026-11-16T03:00:00Z");

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 훈련한_날짜와_출처를_저장하고_조회한다() {
		TrainedLesson trainedLesson = record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT);
		entityManager.persistAndFlush(trainedLesson);
		Long trainedLessonId = trainedLesson.getId();
		entityManager.clear();

		TrainedLesson found = entityManager.find(TrainedLesson.class, trainedLessonId);

		assertThat(found.getTrainedDate()).isEqualTo(LocalDate.of(2026, 11, 16));
		assertThat(found.getSource()).isEqualTo(TrainingSource.PLAN);
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void 같은_강아지의_같은_레슨은_하루에_출처마다_한_행만_저장된다() {
		entityManager.persistAndFlush(record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT));
		TrainedLesson duplicate = record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT.plusSeconds(60));

		assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 출처가_다르면_같은_날_같은_레슨도_따로_저장된다() {
		entityManager.persistAndFlush(record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT));
		TrainedLesson library = record(DOG_ID, LESSON_ID, TrainingSource.LIBRARY, COMPLETED_AT);

		entityManager.persistAndFlush(library);

		assertThat(library.getId()).isNotNull();
	}

	@Test
	void 날짜가_다르면_같은_레슨도_따로_저장된다() {
		entityManager.persistAndFlush(record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT));
		TrainedLesson nextDay = record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT.plusSeconds(86_400));

		entityManager.persistAndFlush(nextDay);

		assertThat(nextDay.getId()).isNotNull();
	}

	@Test
	void 강아지가_다르면_같은_날_같은_레슨도_따로_저장된다() {
		entityManager.persistAndFlush(record(DOG_ID, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT));
		TrainedLesson otherDog = record(3L, LESSON_ID, TrainingSource.PLAN, COMPLETED_AT);

		entityManager.persistAndFlush(otherDog);

		assertThat(otherDog.getId()).isNotNull();
	}

	private TrainedLesson record(Long dogId, Long lessonId, TrainingSource source, Instant completedAt) {
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(dogId, lessonId, source, completedAt);
		return TrainedLesson.record(USER_ID, command);
	}
}
