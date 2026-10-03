package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CurriculumTest {

	@Test
	void 레슨_id를_레슨_순서대로_반환한다() {
		Curriculum curriculum = curriculumWithLessonIds(3L, 1L, 2L);

		List<Long> lessonIds = curriculum.getLessonIds();

		assertThat(lessonIds).containsExactly(3L, 1L, 2L);
	}

	@Test
	void 완료한_레슨_id에_속한_레슨만_센다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L, 3L);

		int completedLessons = curriculum.countCompletedLessons(Set.of(1L, 3L));

		assertThat(completedLessons).isEqualTo(2);
	}

	@Test
	void 다른_커리큘럼의_레슨_id는_세지_않는다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		int completedLessons = curriculum.countCompletedLessons(Set.of(2L, 99L));

		assertThat(completedLessons).isEqualTo(1);
	}

	@Test
	void 레슨이_없으면_완료한_레슨은_0개다() {
		Curriculum curriculum = curriculumWithLessonIds();

		int completedLessons = curriculum.countCompletedLessons(Set.of(1L));

		assertThat(completedLessons).isZero();
	}

	private Curriculum curriculumWithLessonIds(Long... lessonIds) {
		Curriculum curriculum = CurriculumFixture.create(null, "커리큘럼", 1, null, null);
		List<Lesson> lessons = new ArrayList<>();
		for (int i = 0; i < lessonIds.length; i++) {
			Lesson lesson = LessonFixture.create(curriculum, "레슨", i + 1, 5);
			ReflectionTestUtils.setField(lesson, "id", lessonIds[i]);
			lessons.add(lesson);
		}
		ReflectionTestUtils.setField(curriculum, "lessons", lessons);
		return curriculum;
	}
}
