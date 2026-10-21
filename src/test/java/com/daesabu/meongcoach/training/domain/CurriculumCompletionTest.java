package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurriculumCompletionTest {
	@Test
	void 완료한_레슨_id에_속한_레슨만_센다() {
		List<Long> lessonIds = List.of(1L, 2L, 3L);

		int completed = curriculum(lessonIds).completion(progresses(1L, 3L)).completedLessons();

		assertThat(completed).isEqualTo(2);
	}
	@Test
	void 다른_커리큘럼의_레슨_id는_세지_않는다() {
		List<Long> lessonIds = List.of(1L, 2L);

		int completed = curriculum(lessonIds).completion(progresses(2L, 99L)).completedLessons();

		assertThat(completed).isEqualTo(1);
	}
	@Test
	void 레슨이_없으면_완료한_레슨은_0개다() {
		List<Long> lessonIds = List.of();

		int completed = curriculum(lessonIds).completion(progresses(1L)).completedLessons();

		assertThat(completed).isZero();
	}
	@Test
	void 레슨이_없으면_시작_전_상태다() {
		List<Long> lessonIds = List.of();

		ProgressStatus status = curriculum(lessonIds).completion(progresses()).status();

		assertThat(status).isEqualTo(ProgressStatus.NOT_STARTED);
	}
	@Test
	void 일부_레슨만_완료하면_진행_중_상태다() {
		List<Long> lessonIds = List.of(1L, 2L);

		ProgressStatus status = curriculum(lessonIds).completion(progresses(1L)).status();

		assertThat(status).isEqualTo(ProgressStatus.IN_PROGRESS);
	}
	@Test
	void 모든_레슨을_완료하면_완료_상태다() {
		List<Long> lessonIds = List.of(1L, 2L);

		ProgressStatus status = curriculum(lessonIds).completion(progresses(1L, 2L)).status();

		assertThat(status).isEqualTo(ProgressStatus.COMPLETED);
	}

	private Curriculum curriculum(List<Long> lessonIds) {
		Topic topic = TopicFixture.create(10L);
		Curriculum curriculum = CurriculumFixture.create(topic, 100L);
		lessonIds.forEach(id -> LessonFixture.create(curriculum, id));
		return curriculum;
	}

	private LessonProgresses progresses(Long... lessonIds) {
		List<LessonProgress> records = new ArrayList<>();
		for (Long lessonId : lessonIds) {
			LessonProgress progress = LessonProgress.start(1L, lessonId);
			progress.complete();
			records.add(progress);
		}
		return LessonProgresses.from(records);
	}
}
