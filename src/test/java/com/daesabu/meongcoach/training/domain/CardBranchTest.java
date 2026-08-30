package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardBranchTest {

	@Test
	void 생성하면_카드와_다음_카드_ID가_설정된다() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);
		Topic topic = Topic.create(category, new TopicCreateCommand("산책 교육", 1, null, null, null));
		Curriculum curriculum = Curriculum.create(topic, new CurriculumCreateCommand("리드줄 적응", 1, null, null));
		Lesson lesson = Lesson.create(curriculum, new LessonCreateCommand("리드줄 보여주기", 1, 5));
		Card card = Card.create(lesson, new CardCreateCommand("리드줄 냄새 맡게 하기", 1, null));

		CardBranch branch = CardBranch.create(card, 99L);

		assertThat(branch.getCard()).isEqualTo(card);
		assertThat(branch.getNextCardId()).isEqualTo(99L);
	}
}
