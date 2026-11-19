package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TrainedLessonTest {

	@Test
	void 기록하면_대상과_출처가_담긴다() {
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(2L, 10L, TrainingSource.PLAN,
				Instant.parse("2026-11-16T03:00:00Z"));

		TrainedLesson trainedLesson = TrainedLesson.record(1L, command);

		assertThat(trainedLesson.getUserId()).isEqualTo(1L);
		assertThat(trainedLesson.getDogId()).isEqualTo(2L);
		assertThat(trainedLesson.getLessonId()).isEqualTo(10L);
		assertThat(trainedLesson.getSource()).isEqualTo(TrainingSource.PLAN);
		assertThat(trainedLesson.getTrainedDate()).isEqualTo(LocalDate.of(2026, 11, 16));
	}

	@Test
	void 서울_자정_직전에_완료하면_그날로_기록한다() {
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(2L, 10L, TrainingSource.LIBRARY,
				Instant.parse("2026-11-16T14:59:59Z"));

		TrainedLesson trainedLesson = TrainedLesson.record(1L, command);

		assertThat(trainedLesson.getTrainedDate()).isEqualTo(LocalDate.of(2026, 11, 16));
	}

	@Test
	void 서울_자정에_완료하면_다음_날로_기록한다() {
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(2L, 10L, TrainingSource.LIBRARY,
				Instant.parse("2026-11-16T15:00:00Z"));

		TrainedLesson trainedLesson = TrainedLesson.record(1L, command);

		assertThat(trainedLesson.getTrainedDate()).isEqualTo(LocalDate.of(2026, 11, 17));
	}
}
