package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardTest {

	private Lesson createLesson() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);
		Topic topic = Topic.create(category, new TopicCreateCommand("산책 교육", 1, null, null, null));
		Curriculum curriculum = Curriculum.create(topic, new CurriculumCreateCommand("리드줄 적응", 1, null, null));
		return Lesson.create(curriculum, new LessonCreateCommand("리드줄 보여주기", 1, 5));
	}

	@Test
	void 생성하면_레슨과_지시문이_설정된다() {
		Lesson lesson = createLesson();

		Card card = Card.create(lesson, new CardCreateCommand("리드줄 냄새 맡게 하기", 1, "리드줄을 바닥에 두고 냄새를 맡게 하세요."));

		assertThat(card.getLesson()).isEqualTo(lesson);
		assertThat(card.getTitle()).isEqualTo("리드줄 냄새 맡게 하기");
		assertThat(card.getSortOrder()).isEqualTo(1);
		assertThat(card.getInstruction()).isEqualTo("리드줄을 바닥에 두고 냄새를 맡게 하세요.");
		assertThat(lesson.getEstimatedMinutes()).isEqualTo(5);
	}

	@Test
	void 지시문_없이_미디어만_있는_카드를_생성할_수_있다() {
		Card card = Card.create(createLesson(), new CardCreateCommand(null, 2, null));

		assertThat(card.getTitle()).isEmpty();
		assertThat(card.getInstruction()).isEmpty();
	}
}
