package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserLessonProgressTest {

	@Test
	void startInitializesCompletedCountToZero() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getLessonId()).isEqualTo(10L);
		assertThat(progress.getCompletedCount()).isZero();
	}

	@Test
	void increaseCompletedCountAddsOne() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		progress.increaseCompletedCount();
		progress.increaseCompletedCount();

		assertThat(progress.getCompletedCount()).isEqualTo(2);
	}
}
