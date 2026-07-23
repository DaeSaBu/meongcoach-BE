package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LessonCompletionLogTest {

	@Test
	void recordSetsUserLessonAndDog() {
		LessonCompletionLog log = LessonCompletionLog.record(1L, 10L, 5L);

		assertThat(log.getUserId()).isEqualTo(1L);
		assertThat(log.getLessonId()).isEqualTo(10L);
		assertThat(log.getDogId()).isEqualTo(5L);
	}
}
