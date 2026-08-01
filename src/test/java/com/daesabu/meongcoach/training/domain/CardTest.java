package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Card 도메인")
class CardTest {

	private Lesson createLesson() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);
		Topic topic = Topic.create(category, new TopicCreateCommand("산책 교육", 1, null, null, null));
		Curriculum curriculum = Curriculum.create(topic, new CurriculumCreateCommand("리드줄 적응", 1, null, null));
		return Lesson.create(curriculum, new LessonCreateCommand("리드줄 보여주기", 1, 5));
	}

	@Test
	@DisplayName("생성하면 레슨과 지시문이 설정된다")
	void createSetsLessonAndInstruction() {
		Lesson lesson = createLesson();

		Card card = Card.create(lesson, new CardCreateCommand("리드줄 냄새 맡게 하기", 1, "리드줄을 바닥에 두고 냄새를 맡게 하세요."));

		assertThat(card.getLesson()).isEqualTo(lesson);
		assertThat(card.getTitle()).isEqualTo("리드줄 냄새 맡게 하기");
		assertThat(card.getSortOrder()).isEqualTo(1);
		assertThat(card.getInstruction()).isEqualTo("리드줄을 바닥에 두고 냄새를 맡게 하세요.");
		assertThat(lesson.getEstimatedMinutes()).isEqualTo(5);
	}

	@Test
	@DisplayName("지시문 없이 미디어만 있는 카드를 생성할 수 있다")
	void createAllowsMediaOnlyCardWithoutInstruction() {
		Card card = Card.create(createLesson(), new CardCreateCommand(null, 2, null));

		assertThat(card.getTitle()).isEmpty();
		assertThat(card.getInstruction()).isEmpty();
	}
}
