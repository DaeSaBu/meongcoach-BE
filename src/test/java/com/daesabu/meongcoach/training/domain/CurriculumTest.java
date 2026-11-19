package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
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

		int completed = curriculum.countCompletedLessons(completedProgresses(1L, 3L));

		assertThat(completed).isEqualTo(2);
	}

	@Test
	void 다른_커리큘럼의_레슨_id는_세지_않는다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		int completed = curriculum.countCompletedLessons(completedProgresses(2L, 99L));

		assertThat(completed).isEqualTo(1);
	}

	@Test
	void 레슨이_없으면_진행_상태를_계산할_수_없다() {
		Curriculum curriculum = curriculumWithLessonIds();
		LessonProgresses progresses = completedProgresses();

		assertThatThrownBy(() -> curriculum.determineStatus(progresses))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("커리큘럼에는 레슨이 하나 이상 있어야 합니다.");
	}

	@Test
	void 레슨이_없으면_완료_여부를_판단할_수_없다() {
		Curriculum curriculum = curriculumWithLessonIds();
		LessonProgresses progresses = completedProgresses();

		assertThatThrownBy(() -> curriculum.isCompleted(progresses))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 일부_레슨만_완료하면_진행_중_상태다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		ProgressStatus status = curriculum.determineStatus(completedProgresses(1L));

		assertThat(status).isEqualTo(ProgressStatus.IN_PROGRESS);
	}

	@Test
	void 모든_레슨을_완료하면_완료_상태다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		ProgressStatus status = curriculum.determineStatus(completedProgresses(1L, 2L));

		assertThat(status).isEqualTo(ProgressStatus.COMPLETED);
	}

	@Test
	void 제작_전문가가_있으면_전문가를_반환한다() {
		Expert expert = ExpertFixture.create("정은겸", "교수", null, null);
		Curriculum curriculum = CurriculumFixture.create(null, "커리큘럼", 1, null, null, expert);

		assertThat(curriculum.findExpert()).contains(expert);
	}

	@Test
	void 제작_전문가가_없으면_빈_값을_반환한다() {
		Curriculum curriculum = CurriculumFixture.create(null, "커리큘럼", 1, null, null);

		assertThat(curriculum.findExpert()).isEmpty();
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

	private LessonProgresses completedProgresses(Long... lessonIds) {
		List<LessonProgress> progresses = new ArrayList<>();
		for (Long lessonId : lessonIds) {
			LessonProgress progress = LessonProgress.start(1L, 7L, lessonId);
			progress.complete();
			progresses.add(progress);
		}
		return LessonProgresses.from(progresses);
	}
}
