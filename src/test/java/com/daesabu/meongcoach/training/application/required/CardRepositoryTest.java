package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardCreateCommand;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
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
 * 카드 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("카드 리포지토리")
class CardRepositoryTest {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("레슨의 카드를 정렬 순서 오름차순으로 조회한다")
	void findAllByLessonIdOrdersBySortOrderAscending() {
		Lesson lesson = persistLesson("기본 교육");
		persistCard(lesson, "셋째", 3);
		persistCard(lesson, "첫째", 1);
		persistCard(lesson, "둘째", 2);
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lesson.getId());

		assertThat(cards).extracting(Card::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	@DisplayName("정렬 순서가 같으면 id 오름차순으로 조회한다")
	void findAllByLessonIdOrdersByIdAscendingWhenSortOrderIsSame() {
		Lesson lesson = persistLesson("기본 교육");
		Card first = persistCard(lesson, "먼저 등록", 1);
		Card second = persistCard(lesson, "나중 등록", 1);
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lesson.getId());

		assertThat(cards).extracting(Card::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("다른 레슨의 카드는 조회되지 않는다")
	void findAllByLessonIdExcludesOtherLessonCards() {
		Lesson target = persistLesson("기본 교육");
		Lesson other = persistLesson("문제 행동");
		persistCard(target, "대상 카드", 1);
		persistCard(other, "다른 카드", 1);
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(target.getId());

		assertThat(cards).extracting(Card::getTitle)
				.containsExactly("대상 카드");
	}

	@Test
	@DisplayName("카드가 없는 레슨이면 빈 목록을 반환한다")
	void findAllByLessonIdReturnsEmptyListWhenLessonHasNoCard() {
		Lesson lesson = persistLesson("기본 교육");
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lesson.getId());

		assertThat(cards).isEmpty();
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1)));
		CurriculumCreateCommand curriculumCommand = new CurriculumCreateCommand(title + " 커리큘럼", 1, null, null);
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic, curriculumCommand));
		return entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand(title + " 레슨", 1, 5)));
	}

	private Card persistCard(Lesson lesson, String title, int sortOrder) {
		CardCreateCommand command = new CardCreateCommand(title, sortOrder, "지시문");
		return entityManager.persist(Card.create(lesson, command));
	}
}
