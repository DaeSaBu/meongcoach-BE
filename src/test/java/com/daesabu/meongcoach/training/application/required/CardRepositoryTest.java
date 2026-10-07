package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardFixture;
import com.daesabu.meongcoach.training.domain.CardMedia;
import com.daesabu.meongcoach.training.domain.CardMediaFixture;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.MediaType;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class CardRepositoryTest {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 카드의_미디어를_정렬_순서_오름차순으로_로딩하고_다른_카드의_미디어는_제외한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card card = persistCard(lesson, "카드", 1);
		Card other = persistCard(lesson, "다른 카드", 2);
		persistCardMedia(card, "https://cdn.example.com/3.png", 3);
		persistCardMedia(card, "https://cdn.example.com/1.png", 1);
		persistCardMedia(card, "https://cdn.example.com/2.png", 2);
		persistCardMedia(other, "https://cdn.example.com/other.png", 1);
		flushAndClear();

		Card found = cardRepository.findById(card.getId()).orElseThrow();

		assertThat(found.getCardMedia()).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/1.png", "https://cdn.example.com/2.png",
						"https://cdn.example.com/3.png");
	}

	@Test
	void 정렬_순서가_같은_미디어는_id_오름차순으로_로딩한다() {
		Card card = persistCard(persistLesson("기본 교육"), "카드", 1);
		CardMedia first = persistCardMedia(card, "https://cdn.example.com/first.png", 1);
		CardMedia second = persistCardMedia(card, "https://cdn.example.com/second.png", 1);
		flushAndClear();

		Card found = cardRepository.findById(card.getId()).orElseThrow();

		assertThat(found.getCardMedia()).extracting(CardMedia::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 미디어가_없는_카드는_빈_미디어_목록으로_로딩한다() {
		Card card = persistCard(persistLesson("기본 교육"), "미디어 없는 카드", 1);
		flushAndClear();

		Card found = cardRepository.findById(card.getId()).orElseThrow();

		assertThat(found.getCardMedia()).isEmpty();
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

	private CardMedia persistCardMedia(Card card, String url, int sortOrder) {
		return entityManager.persist(CardMediaFixture.create(card, MediaType.IMAGE, url, sortOrder));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
