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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 카드 미디어 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("카드 미디어 리포지토리")
class CardMediaRepositoryTest {

	@Autowired
	private CardMediaRepository cardMediaRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("여러 카드의 미디어를 정렬 순서 오름차순으로 조회한다")
	void findAllByCardIdInOrdersBySortOrderAscending() {
		Lesson lesson = persistLesson("기본 훈련");
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
	@DisplayName("정렬 순서가 같으면 id 오름차순으로 조회한다")
	void findAllByCardIdInOrdersByIdAscendingWhenSortOrderIsSame() {
		Lesson lesson = persistLesson("기본 훈련");
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
	@DisplayName("조회 대상에 없는 카드의 미디어는 조회되지 않는다")
	void findAllByCardIdInExcludesMediaOfOtherCards() {
		Lesson lesson = persistLesson("기본 훈련");
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
	@DisplayName("카드 id 목록이 비어 있으면 빈 목록을 반환한다")
	void findAllByCardIdInReturnsEmptyListWhenIdsAreEmpty() {
		Lesson lesson = persistLesson("기본 훈련");
		Card card = persistCard(lesson, "카드", 1);
		persistCardMedia(card, "https://cdn.example.com/1.png", 1);
		entityManager.flush();

		List<CardMedia> cardMedia = cardMediaRepository.findAllByCard_IdInOrderBySortOrderAscIdAsc(List.of());

		assertThat(cardMedia).isEmpty();
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1)));
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
