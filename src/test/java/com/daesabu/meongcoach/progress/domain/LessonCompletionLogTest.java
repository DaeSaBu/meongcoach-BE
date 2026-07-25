package com.daesabu.meongcoach.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LessonCompletionLog 도메인")
class LessonCompletionLogTest {

	@Test
	@DisplayName("기록하면 사용자·레슨·강아지 정보가 설정된다")
	void recordSetsUserLessonAndDog() {
		LessonCompletionLog log = LessonCompletionLog.record(new LessonCompletionLogRecordCommand(1L, 10L, 5L));

		assertThat(log.getUserId()).isEqualTo(1L);
		assertThat(log.getLessonId()).isEqualTo(10L);
		assertThat(log.getDogId()).isEqualTo(5L);
	}
}
