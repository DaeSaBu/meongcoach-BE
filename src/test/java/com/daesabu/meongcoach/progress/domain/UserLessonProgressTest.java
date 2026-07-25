package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserLessonProgress 도메인")
class UserLessonProgressTest {

	@Test
	@DisplayName("시작하면 완료 횟수가 0으로 초기화된다")
	void startInitializesCompletedCountToZero() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getLessonId()).isEqualTo(10L);
		assertThat(progress.getCompletedCount()).isZero();
	}

	@Test
	@DisplayName("완료 횟수를 올리면 1씩 증가한다")
	void increaseCompletedCountAddsOne() {
		UserLessonProgress progress = UserLessonProgress.start(1L, 10L);

		progress.increaseCompletedCount();
		progress.increaseCompletedCount();

		assertThat(progress.getCompletedCount()).isEqualTo(2);
	}
}
