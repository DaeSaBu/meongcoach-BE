package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CurriculumStatus 커리큘럼 진행 상태")
class CurriculumStatusTest {

	@Test
	@DisplayName("레슨이 하나도 없으면 시작 전이다")
	void statusIsNotStartedWhenThereIsNoLesson() {
		CurriculumStatus status = CurriculumStatus.of(0, 0);

		assertThat(status).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("완료한 레슨이 없으면 시작 전이다")
	void statusIsNotStartedWhenNoLessonIsCompleted() {
		CurriculumStatus status = CurriculumStatus.of(5, 0);

		assertThat(status).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("일부 레슨만 완료하면 진행 중이다")
	void statusIsInProgressWhenSomeLessonsAreCompleted() {
		CurriculumStatus status = CurriculumStatus.of(5, 2);

		assertThat(status).isEqualTo(CurriculumStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("모든 레슨을 완료하면 완료다")
	void statusIsCompletedWhenAllLessonsAreCompleted() {
		CurriculumStatus status = CurriculumStatus.of(5, 5);

		assertThat(status).isEqualTo(CurriculumStatus.COMPLETED);
	}
}
