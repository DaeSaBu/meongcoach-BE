package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainedLessonsTest {

	private static final LocalDate FIRST_DAY = LocalDate.of(2026, 11, 14);

	private static final LocalDate SECOND_DAY = LocalDate.of(2026, 11, 15);

	private static final LocalDate THIRD_DAY = LocalDate.of(2026, 11, 16);

	private static final Long LESSON_ID = 10L;

	private static final Long OTHER_LESSON_ID = 11L;

	@Test
	void 기간_안에서_훈련한_날짜를_겹치지_않게_오름차순으로_찾는다() {
		TrainedLessons trainedLessons = new TrainedLessons(List.of(
				record(LESSON_ID, THIRD_DAY, TrainingSource.PLAN),
				record(LESSON_ID, FIRST_DAY, TrainingSource.PLAN),
				record(OTHER_LESSON_ID, SECOND_DAY, TrainingSource.LIBRARY),
				record(LESSON_ID, SECOND_DAY, TrainingSource.PLAN)));
		TrainingCalendarPeriod period = new TrainingCalendarPeriod(SECOND_DAY, THIRD_DAY);

		List<LocalDate> trainedDates = trainedLessons.findTrainedDatesIn(period);

		assertThat(trainedDates).containsExactly(SECOND_DAY, THIRD_DAY);
	}

	@Test
	void 그날_그_출처로_훈련한_레슨만_찾는다() {
		TrainedLessons trainedLessons = new TrainedLessons(List.of(
				record(LESSON_ID, FIRST_DAY, TrainingSource.PLAN),
				record(OTHER_LESSON_ID, FIRST_DAY, TrainingSource.LIBRARY),
				record(OTHER_LESSON_ID, SECOND_DAY, TrainingSource.PLAN)));

		assertThat(trainedLessons.findLessonIdsOn(FIRST_DAY, TrainingSource.PLAN)).containsExactly(LESSON_ID);
		assertThat(trainedLessons.findLessonIdsOn(FIRST_DAY, TrainingSource.LIBRARY))
				.containsExactly(OTHER_LESSON_ID);
		assertThat(trainedLessons.findLessonIdsOn(THIRD_DAY, TrainingSource.PLAN)).isEmpty();
	}

	@Test
	void 그날까지_레슨을_훈련한_날_수를_센다() {
		TrainedLessons trainedLessons = new TrainedLessons(List.of(
				record(LESSON_ID, FIRST_DAY, TrainingSource.PLAN),
				record(LESSON_ID, SECOND_DAY, TrainingSource.PLAN),
				record(LESSON_ID, THIRD_DAY, TrainingSource.PLAN),
				record(OTHER_LESSON_ID, FIRST_DAY, TrainingSource.PLAN)));

		assertThat(trainedLessons.countTrainedDaysUntil(LESSON_ID, FIRST_DAY)).isOne();
		assertThat(trainedLessons.countTrainedDaysUntil(LESSON_ID, SECOND_DAY)).isEqualTo(2);
		assertThat(trainedLessons.countTrainedDaysUntil(LESSON_ID, THIRD_DAY)).isEqualTo(3);
	}

	@Test
	void 같은_날_플랜과_라이브러리로_모두_훈련해도_하루로_센다() {
		TrainedLessons trainedLessons = new TrainedLessons(List.of(
				record(LESSON_ID, FIRST_DAY, TrainingSource.PLAN),
				record(LESSON_ID, FIRST_DAY, TrainingSource.LIBRARY)));

		assertThat(trainedLessons.countTrainedDaysUntil(LESSON_ID, FIRST_DAY)).isOne();
	}

	@Test
	void 훈련한_적_없는_레슨의_날_수는_0이다() {
		TrainedLessons trainedLessons = new TrainedLessons(List.of(
				record(LESSON_ID, SECOND_DAY, TrainingSource.PLAN)));

		assertThat(trainedLessons.countTrainedDaysUntil(OTHER_LESSON_ID, THIRD_DAY)).isZero();
		assertThat(trainedLessons.countTrainedDaysUntil(LESSON_ID, FIRST_DAY)).isZero();
	}

	private TrainedLesson record(Long lessonId, LocalDate trainedDate, TrainingSource source) {
		Instant completedAt = Instant.parse(trainedDate + "T03:00:00Z");
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(2L, lessonId, source, completedAt);
		return TrainedLesson.record(1L, command);
	}
}
