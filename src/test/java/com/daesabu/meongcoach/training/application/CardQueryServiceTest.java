package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardCreateCommand;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CardQueryService.class)
class CardQueryServiceTest {

	private static final Long ABSENT_CARD_ID = 999L;

	@Autowired
	private CardFinder cardFinder;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 저장된_카드는_존재한다() {
		Card card = persistCard();
		entityManager.flush();
		entityManager.clear();

		assertThat(cardFinder.existsCard(card.getId())).isTrue();
	}

	@Test
	void 없는_카드는_존재하지_않는다() {
		assertThat(cardFinder.existsCard(ABSENT_CARD_ID)).isFalse();
	}

	private Card persistCard() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("카테고리", 1, null, null));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand("토픽", 1, null, null, null)));
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic,
				new CurriculumCreateCommand("커리큘럼", 1, null, null)));
		Lesson lesson = entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand("레슨", 1, 5)));
		return entityManager.persist(Card.create(lesson, new CardCreateCommand("카드", 1, "지시문")));
	}
}
