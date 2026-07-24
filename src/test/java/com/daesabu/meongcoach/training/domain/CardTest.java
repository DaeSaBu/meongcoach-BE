package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardTest {

	private Lesson createLesson() {
		TrainingCategory category = TrainingCategory.create("기본 훈련", 1);
		Topic topic = Topic.create(category, "산책 훈련", 1);
		Curriculum curriculum = Curriculum.create(topic, "리드줄 적응", 1, null);
		return Lesson.create(curriculum, "리드줄 보여주기", 1, 5);
	}

	@Test
	void createSetsLessonAndInstruction() {
		Lesson lesson = createLesson();

		Card card = Card.create(lesson, "리드줄 냄새 맡게 하기", 1, "리드줄을 바닥에 두고 냄새를 맡게 하세요.");

		assertThat(card.getLesson()).isEqualTo(lesson);
		assertThat(card.getTitle()).isEqualTo("리드줄 냄새 맡게 하기");
		assertThat(card.getSortOrder()).isEqualTo(1);
		assertThat(card.getInstruction()).isEqualTo("리드줄을 바닥에 두고 냄새를 맡게 하세요.");
		assertThat(lesson.getEstimatedMinutes()).isEqualTo(5);
	}

	@Test
	void createAllowsMediaOnlyCardWithoutInstruction() {
		Card card = Card.create(createLesson(), null, 2, null);

		assertThat(card.getTitle()).isNull();
		assertThat(card.getInstruction()).isNull();
	}
}
