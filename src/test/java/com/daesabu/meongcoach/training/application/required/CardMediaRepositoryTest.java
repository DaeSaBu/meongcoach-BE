package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardCreateCommand;
import com.daesabu.meongcoach.training.domain.CardMedia;
import com.daesabu.meongcoach.training.domain.CardMediaCreateCommand;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.MediaType;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 카드 미디어 조회 리포지토리 검증.
 */
@DataJpaTest
class CardMediaRepositoryTest {

	@Autowired
	private CardMediaRepository cardMediaRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 여러_카드의_미디어를_정렬_순서_오름차순으로_조회한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card first = persistCard(lesson, "첫째 카드", 1);
		Card second = persistCard(lesson, "둘째 카드", 2);
		persistCardMedia(first, "https://cdn.example.com/3.png", 3);
		persistCardMedia(second, "https://cdn.example.com/2.png", 2);
		persistCardMedia(first, "https://cdn.example.com/1.png", 1);
		entityManager.flush();

		List<CardMedia> cardMedia = cardMediaRepository
				.findAllByCard_IdInOrderBySortOrderAscIdAsc(List.of(first.getId(), second.getId()));

		assertThat(cardMedia).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/1.png", "https://cdn.example.com/2.png",
						"https://cdn.example.com/3.png");
	}

	@Test
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card card = persistCard(lesson, "카드", 1);
		CardMedia first = persistCardMedia(card, "https://cdn.example.com/first.png", 1);
		CardMedia second = persistCardMedia(card, "https://cdn.example.com/second.png", 1);
		entityManager.flush();

		List<CardMedia> cardMedia = cardMediaRepository
				.findAllByCard_IdInOrderBySortOrderAscIdAsc(List.of(card.getId()));

		assertThat(cardMedia).extracting(CardMedia::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 조회_대상에_없는_카드의_미디어는_조회되지_않는다() {
		Lesson lesson = persistLesson("기본 교육");
		Card target = persistCard(lesson, "대상 카드", 1);
		Card other = persistCard(lesson, "다른 카드", 2);
		persistCardMedia(target, "https://cdn.example.com/target.png", 1);
		persistCardMedia(other, "https://cdn.example.com/other.png", 1);
		entityManager.flush();

		List<CardMedia> cardMedia = cardMediaRepository
				.findAllByCard_IdInOrderBySortOrderAscIdAsc(List.of(target.getId()));

		assertThat(cardMedia).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/target.png");
	}

	@Test
	void 카드_id_목록이_비어_있으면_빈_목록을_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card card = persistCard(lesson, "카드", 1);
		persistCardMedia(card, "https://cdn.example.com/1.png", 1);
		entityManager.flush();

		List<CardMedia> cardMedia = cardMediaRepository.findAllByCard_IdInOrderBySortOrderAscIdAsc(List.of());

		assertThat(cardMedia).isEmpty();
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1, null, null));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1, null, null, null)));
		CurriculumCreateCommand curriculumCommand = new CurriculumCreateCommand(title + " 커리큘럼", 1, null, null);
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic, curriculumCommand));
		return entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand(title + " 레슨", 1, 5)));
	}

	private Card persistCard(Lesson lesson, String title, int sortOrder) {
		return entityManager.persist(Card.create(lesson, new CardCreateCommand(title, sortOrder, "지시문")));
	}

	private CardMedia persistCardMedia(Card card, String url, int sortOrder) {
		CardMediaCreateCommand command = new CardMediaCreateCommand(MediaType.IMAGE, url, sortOrder);
		return entityManager.persist(CardMedia.create(card, command));
	}
}
