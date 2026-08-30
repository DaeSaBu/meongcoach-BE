package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserLessonProgressTest {

	@Test
	void 시작하면_완료_횟수가_0으로_초기화된다() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getLessonId()).isEqualTo(10L);
		assertThat(progress.getCompletedCount()).isZero();
	}

	@Test
	void 완료_횟수를_올리면_1씩_증가한다() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		progress.increaseCompletedCount();
		progress.increaseCompletedCount();

		assertThat(progress.getCompletedCount()).isEqualTo(2);
	}
}
