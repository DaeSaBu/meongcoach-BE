package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
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
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CardFinderTest {

	@Autowired
	private CardFinder cardFinder;

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
	private EntityManager entityManager;

	@Test
	void 카테고리부터_카드까지_각_계층의_id와_제목을_요약으로_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card card = saveCard(lesson, "앉아 준비", 1);
		flushAndClear();

		List<CardSummaryResult> summaries = cardFinder.findSummaries(Set.of(card.getId()));

		Curriculum curriculum = lesson.getCurriculum();
		Topic topic = curriculum.getTopic();
		TrainingCategory category = topic.getTrainingCategory();
		assertThat(summaries).containsExactly(new CardSummaryResult(
				category.getId(), "기본 교육 카테고리",
				topic.getId(), "기본 교육",
				curriculum.getId(), "기본 교육 커리큘럼",
				lesson.getId(), "기본 교육 레슨",
				card.getId(), "앉아 준비"));
	}

	@Test
	void 여러_카드의_요약을_한_번에_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		Card first = saveCard(lesson, "앉아 준비", 1);
		Card second = saveCard(lesson, "앉아 실전", 2);
		flushAndClear();

		List<CardSummaryResult> summaries = cardFinder.findSummaries(Set.of(first.getId(), second.getId()));

		assertThat(summaries).extracting(CardSummaryResult::title)
				.containsExactlyInAnyOrder("앉아 준비", "앉아 실전");
	}

	@Test
	void 존재하지_않는_카드_id는_결과에서_빠진다() {
		Lesson lesson = saveLesson("기본 교육");
		Card card = saveCard(lesson, "앉아 준비", 1);
		flushAndClear();

		List<CardSummaryResult> summaries = cardFinder.findSummaries(Set.of(card.getId(), -1L));

		assertThat(summaries).extracting(CardSummaryResult::id).containsExactly(card.getId());
	}

	@Test
	void 빈_id_목록이면_빈_목록을_반환한다() {
		List<CardSummaryResult> summaries = cardFinder.findSummaries(Set.of());

		assertThat(summaries).isEmpty();
	}

	private Lesson saveLesson(String title) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create(title + " 카테고리", 1, null, null));
		Topic topic = topicRepository.save(TopicFixture.create(category, title, 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(
				CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null));
		return lessonRepository.save(LessonFixture.create(curriculum, title + " 레슨", 1, 5));
	}

	private Card saveCard(Lesson lesson, String title, int sortOrder) {
		return cardRepository.save(CardFixture.create(lesson, title, sortOrder, "지시문"));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
