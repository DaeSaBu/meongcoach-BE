package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LessonProgressesTest {
	@Test
	void 미완료_레슨의_기록은_완료로_세지_않는다() {
		LessonProgress uncompleted = LessonProgress.start(1L, 1001L);
		LessonProgresses progresses = LessonProgresses.from(List.of(uncompleted));

		assertThat(progresses.countCompletedIn(List.of(1001L)))
				.isZero();
	}

	@Test
	void 반복_완료도_하나의_레슨으로_센다() {
		LessonProgress record = LessonProgress.start(1L, 1001L);
		record.complete();
		record.complete();
		LessonProgresses progresses = LessonProgresses.from(List.of(record));

		assertThat(progresses.countCompletedIn(List.of(1001L, 1002L)))
				.isEqualTo(1);
	}

	@Test
	void 레슨별_완료_횟수를_반환하고_미기록은_0이다() {
		LessonProgress twice = LessonProgress.start(1L, 1001L);
		twice.complete();
		twice.complete();
		LessonProgress uncompleted = LessonProgress.start(1L, 1002L);
		LessonProgress other = LessonProgress.start(1L, 9999L);
		other.complete();
		LessonProgresses progresses = LessonProgresses.from(List.of(twice, uncompleted, other));

		assertThat(progresses.completedCount(1003L)).isZero();
		assertThat(progresses.completedCount(1001L)).isEqualTo(2);
		assertThat(progresses.completedCount(1002L)).isZero();
	}

	@Test
	void 진행_기록이_없으면_완료_횟수와_개수는_0이다() {
		LessonProgresses progresses = LessonProgresses.from(List.of());

		assertThat(progresses.completedCount(1001L)).isZero();
		assertThat(progresses.countCompletedIn(List.of())).isZero();
	}
}
