package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
	void 요청한_레슨_순서로_완료_횟수를_반환하고_미기록은_0으로_채운다() {
		LessonProgress twice = LessonProgress.start(1L, 1001L);
		twice.complete();
		twice.complete();
		LessonProgress uncompleted = LessonProgress.start(1L, 1002L);
		LessonProgress other = LessonProgress.start(1L, 9999L);
		other.complete();
		LessonProgresses progresses = LessonProgresses.from(List.of(twice, uncompleted, other));

		assertThat(progresses.completedCounts(List.of(1003L, 1001L, 1002L, 1001L)))
				.containsExactly(entry(1003L, 0), entry(1001L, 2), entry(1002L, 0));
	}

	@Test
	void 조회할_레슨이_없으면_완료_횟수도_비어_있다() {
		LessonProgresses progresses = LessonProgresses.from(List.of());

		assertThat(progresses.completedCounts(List.of())).isEmpty();
		assertThat(progresses.countCompletedIn(List.of())).isZero();
	}
}
