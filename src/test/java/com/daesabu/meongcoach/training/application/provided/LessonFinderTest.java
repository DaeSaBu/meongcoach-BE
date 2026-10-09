package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
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
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LessonFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private LessonFinder lessonFinder;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 카드를_정렬_순서_오름차순으로_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		saveCard(lesson, "나중 카드 지시문", 2);
		saveCard(lesson, "먼저 카드 지시문", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).extracting(Card::getInstruction)
				.containsExactly("먼저 카드 지시문", "나중 카드 지시문");
	}

	@Test
	void 카드_타이틀을_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		saveCard(lesson, "앉아 준비", "지시문", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).extracting(Card::getTitle).containsExactly("앉아 준비");
	}

	@Test
	void 카드_안의_미디어를_정렬_순서_오름차순으로_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card card = saveCard(lesson, "지시문", 1);
		saveCardMedia(card, MediaType.IMAGE, "https://cdn.example.com/3.png", 3);
		saveCardMedia(card, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		saveCardMedia(card, MediaType.VIDEO, "https://cdn.example.com/2.mp4", 2);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).hasSize(1);
		assertThat(cards.getFirst().getCardMedia()).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/1.png", "https://cdn.example.com/2.mp4",
						"https://cdn.example.com/3.png");
	}

	@Test
	void 미디어를_소속_카드에_담아_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card first = saveCard(lesson, "첫째 지시문", 1);
		Card second = saveCard(lesson, "둘째 지시문", 2);
		saveCardMedia(first, MediaType.IMAGE, "https://cdn.example.com/first.png", 1);
		saveCardMedia(second, MediaType.VIDEO, "https://cdn.example.com/second.mp4", 1);
		saveCardMedia(second, MediaType.IMAGE, "https://cdn.example.com/second.png", 2);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).hasSize(2);
		assertThat(cards.get(0).getCardMedia()).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/first.png");
		assertThat(cards.get(1).getCardMedia()).extracting(CardMedia::getUrl)
				.containsExactly("https://cdn.example.com/second.mp4", "https://cdn.example.com/second.png");
	}

	@Test
	void 미디어_유형과_소속_카드_id를_그대로_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card card = saveCard(lesson, "지시문", 1);
		saveCardMedia(card, MediaType.VIDEO, "https://cdn.example.com/1.mp4", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		CardMedia cardMedia = cards.getFirst().getCardMedia().getFirst();
		assertThat(cardMedia.getMediaType()).isEqualTo(MediaType.VIDEO);
		assertThat(cardMedia.getCard().getId()).isEqualTo(card.getId());
	}

	@Test
	void 미디어가_없는_카드는_빈_미디어_목록을_갖는다() {
		Lesson lesson = saveLesson("기본 교육");
		saveCard(lesson, "미디어 없는 지시문", 1);
		Card other = saveCard(lesson, "미디어 있는 지시문", 2);
		saveCardMedia(other, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).hasSize(2);
		assertThat(cards.getFirst().getCardMedia()).isEmpty();
	}

	@Test
	void 다른_레슨의_카드는_조회되지_않는다() {
		Lesson lesson = saveLesson("기본 교육");
		Lesson other = saveLesson("심화 교육");
		saveCard(lesson, "대상 지시문", 1);
		saveCard(other, "다른 레슨 지시문", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).extracting(Card::getInstruction).containsExactly("대상 지시문");
	}

	@Test
	void 카드가_없는_레슨은_빈_목록을_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).isEmpty();
	}

	@Test
	void 존재하지_않는_레슨이면_예외를_던진다() {
		assertThatThrownBy(() -> lessonFinder.findCards(USER_ID, -1L))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	void 카드를_조회할_때_미디어도_함께_로딩한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card first = saveCard(lesson, "첫째 지시문", 1);
		Card second = saveCard(lesson, "둘째 지시문", 2);
		saveCardMedia(first, MediaType.IMAGE, "https://cdn.example.com/1.png", 1);
		saveCardMedia(second, MediaType.VIDEO, "https://cdn.example.com/2.mp4", 1);
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).allSatisfy(card -> assertThat(Hibernate.isInitialized(card.getCardMedia())).isTrue());
	}

	@Test
	void 이용권_없이_유료_레슨의_카드를_조회하면_예외를_던진다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		flushAndClear();

		assertThatThrownBy(() -> lessonFinder.findCards(USER_ID, lesson.getId()))
				.isInstanceOf(EntitlementRequiredException.class);
	}

	@Test
	void 이용권이_있으면_유료_레슨의_카드를_조회한다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		saveCard(lesson, "지시문", 1);
		entitlementRepository.save(EntitlementFixture.create(USER_ID, EntitlementType.PUPPY, null));
		flushAndClear();

		List<Card> cards = lessonFinder.findCards(USER_ID, lesson.getId());

		assertThat(cards).extracting(Card::getInstruction).containsExactly("지시문");
	}

	private Lesson saveLesson(String title) {
		return saveLesson(title, null, false);
	}

	private Lesson savePremiumLesson(String title, EntitlementType requiredEntitlementType) {
		return saveLesson(title, requiredEntitlementType, true);
	}

	private Lesson saveLesson(String title, EntitlementType requiredEntitlementType, boolean isPremium) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create(title + " 카테고리", 1, null, null, requiredEntitlementType));
		Topic topic = topicRepository.save(TopicFixture.create(category, title, 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(
				CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null, isPremium));
		return lessonRepository.save(LessonFixture.create(curriculum, title + " 레슨", 1, 5));
	}

	private Card saveCard(Lesson lesson, String instruction, int sortOrder) {
		return saveCard(lesson, "카드", instruction, sortOrder);
	}

	private Card saveCard(Lesson lesson, String title, String instruction, int sortOrder) {
		return cardRepository.save(CardFixture.create(lesson, title, sortOrder, instruction));
	}

	private void saveCardMedia(Card card, MediaType mediaType, String url, int sortOrder) {
		entityManager.persist(CardMediaFixture.create(card, mediaType, url, sortOrder));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
