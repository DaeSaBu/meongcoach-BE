package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.application.provided.CardMediaResult;
import com.daesabu.meongcoach.training.application.provided.CardResult;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
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
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 레슨 카드 조회 서비스 검증.
 */
@DataJpaTest
@Import(LessonQueryService.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class LessonQueryServiceTest {

	@Autowired
	private LessonFinder lessonFinder;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void 카드를_정렬_순서_오름차순으로_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		persistCard(lesson, "나중 카드 지시문", 2);
		persistCard(lesson, "먼저 카드 지시문", 1);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).extracting(CardResult::instruction)
				.containsExactly("먼저 카드 지시문", "나중 카드 지시문");
	}

	@Test
	void 카드_타이틀을_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		persistCard(lesson, "앉아 준비", "지시문", 1);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).extracting(CardResult::title).containsExactly("앉아 준비");
	}

	@Test
	void 카드_안의_미디어를_정렬_순서_오름차순으로_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card card = persistCard(lesson, "지시문", 1);
		persistCardMedia(card, MediaType.IMAGE, "https://cdn.example.com/3.png", 3);
		persistCardMedia(card, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		persistCardMedia(card, MediaType.VIDEO, "https://cdn.example.com/2.mp4", 2);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).hasSize(1);
		assertThat(cards.getFirst().cardMedia()).extracting(CardMediaResult::url)
				.containsExactly("https://cdn.example.com/1.png", "https://cdn.example.com/2.mp4",
						"https://cdn.example.com/3.png");
	}

	@Test
	void 미디어를_소속_카드에_담아_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card first = persistCard(lesson, "첫째 지시문", 1);
		Card second = persistCard(lesson, "둘째 지시문", 2);
		persistCardMedia(first, MediaType.IMAGE, "https://cdn.example.com/first.png", 1);
		persistCardMedia(second, MediaType.VIDEO, "https://cdn.example.com/second.mp4", 1);
		persistCardMedia(second, MediaType.IMAGE, "https://cdn.example.com/second.png", 2);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).hasSize(2);
		assertThat(cards.get(0).cardMedia()).extracting(CardMediaResult::url)
				.containsExactly("https://cdn.example.com/first.png");
		assertThat(cards.get(1).cardMedia()).extracting(CardMediaResult::url)
				.containsExactly("https://cdn.example.com/second.mp4", "https://cdn.example.com/second.png");
	}

	@Test
	void 미디어_유형과_소속_카드_id를_그대로_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card card = persistCard(lesson, "지시문", 1);
		persistCardMedia(card, MediaType.VIDEO, "https://cdn.example.com/1.mp4", 1);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		CardMediaResult cardMedia = cards.getFirst().cardMedia().getFirst();
		assertThat(cardMedia.mediaType()).isEqualTo(MediaType.VIDEO);
		assertThat(cardMedia.cardId()).isEqualTo(card.getId());
	}

	@Test
	void 미디어가_없는_카드는_빈_미디어_목록을_갖는다() {
		Lesson lesson = persistLesson("기본 교육");
		persistCard(lesson, "미디어 없는 지시문", 1);
		Card other = persistCard(lesson, "미디어 있는 지시문", 2);
		persistCardMedia(other, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).hasSize(2);
		assertThat(cards.getFirst().cardMedia()).isEmpty();
	}

	@Test
	void 다른_레슨의_카드는_조회되지_않는다() {
		Lesson lesson = persistLesson("기본 교육");
		Lesson other = persistLesson("심화 교육");
		persistCard(lesson, "대상 지시문", 1);
		persistCard(other, "다른 레슨 지시문", 1);
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).extracting(CardResult::instruction).containsExactly("대상 지시문");
	}

	@Test
	void 카드가_없는_레슨은_빈_목록을_반환한다() {
		Lesson lesson = persistLesson("기본 교육");
		flushAndClear();

		List<CardResult> cards = lessonFinder.findCards(lesson.getId());

		assertThat(cards).isEmpty();
	}

	@Test
	void 존재하지_않는_레슨이면_예외를_던진다() {
		flushAndClear();

		assertThatThrownBy(() -> lessonFinder.findCards(-1L))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	void 카드_수와_무관하게_세_번의_쿼리로_조회한다() {
		Lesson lesson = persistLesson("기본 교육");
		Card first = persistCard(lesson, "첫째 지시문", 1);
		Card second = persistCard(lesson, "둘째 지시문", 2);
		persistCardMedia(first, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		persistCardMedia(second, MediaType.VIDEO, "https://cdn.example.com/2.mp4", 1);
		flushAndClear();
		Statistics statistics = clearedStatistics();

		lessonFinder.findCards(lesson.getId());

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1, null, null));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1, null, null, null)));
		CurriculumCreateCommand curriculumCommand = new CurriculumCreateCommand(title + " 커리큘럼", 1, null, null);
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic, curriculumCommand));
		return entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand(title + " 레슨", 1, 5)));
	}

	private Card persistCard(Lesson lesson, String instruction, int sortOrder) {
		return persistCard(lesson, "카드", instruction, sortOrder);
	}

	private Card persistCard(Lesson lesson, String title, String instruction, int sortOrder) {
		CardCreateCommand command = new CardCreateCommand(title, sortOrder, instruction);
		return entityManager.persist(Card.create(lesson, command));
	}

	private CardMedia persistCardMedia(Card card, MediaType mediaType, String url, int sortOrder) {
		CardMediaCreateCommand command = new CardMediaCreateCommand(mediaType, url, sortOrder);
		return entityManager.persist(CardMedia.create(card, command));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private Statistics clearedStatistics() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		return statistics;
	}
}
