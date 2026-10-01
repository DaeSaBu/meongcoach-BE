package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardFixture;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 카드 조회 리포지토리 검증.
 */
@DataJpaTest
class CardRepositoryTest {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 레슨의_카드를_정렬_순서_오름차순으로_조회한다() {
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
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card first = persistCard(lesson, "먼저 등록", 1);
		Card second = persistCard(lesson, "나중 등록", 1);
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lesson.getId());

		assertThat(cards).extracting(Card::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 다른_레슨의_카드는_조회되지_않는다() {
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
	void 카드가_없는_레슨이면_빈_목록을_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		entityManager.flush();

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lesson.getId());

		assertThat(cards).isEmpty();
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategoryFixture.create(title + " 카테고리", 1, null, null));
		Topic topic = entityManager.persist(TopicFixture.create(category, title, 1, null, null, null));
		Curriculum curriculum = entityManager.persist(CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null));
		return entityManager.persist(LessonFixture.create(curriculum, title + " 레슨", 1, 5));
	}

	private Card persistCard(Lesson lesson, String title, int sortOrder) {
		return entityManager.persist(CardFixture.create(lesson, title, sortOrder, "지시문"));
	}
}
