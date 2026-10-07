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
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class LessonRepositoryTest {

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 레슨의_카드를_정렬_순서_오름차순으로_로딩하고_다른_레슨의_카드는_제외한다() {
		Curriculum curriculum = persistCurriculum("기본 교육");
		Lesson lesson = persistLesson(curriculum, "레슨");
		Lesson other = persistLesson(curriculum, "다른 레슨");
		persistCard(lesson, "셋째", 3);
		persistCard(lesson, "첫째", 1);
		persistCard(lesson, "둘째", 2);
		persistCard(other, "다른 레슨 카드", 1);
		flushAndClear();

		Lesson found = lessonRepository.findById(lesson.getId()).orElseThrow();

		assertThat(found.getCards()).extracting(Card::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 정렬_순서가_같은_카드는_id_오름차순으로_로딩한다() {
		Lesson lesson = persistLesson(persistCurriculum("기본 교육"), "레슨");
		Card first = persistCard(lesson, "먼저 등록", 1);
		Card second = persistCard(lesson, "나중 등록", 1);
		flushAndClear();

		Lesson found = lessonRepository.findById(lesson.getId()).orElseThrow();

		assertThat(found.getCards()).extracting(Card::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 카드가_없는_레슨은_빈_카드_목록으로_로딩한다() {
		Lesson lesson = persistLesson(persistCurriculum("기본 교육"), "레슨");
		flushAndClear();

		Lesson found = lessonRepository.findById(lesson.getId()).orElseThrow();

		assertThat(found.getCards()).isEmpty();
	}

	@Test
	void 레슨을_커리큘럼_토픽_카테고리와_함께_조회한다() {
		Lesson lesson = persistLesson(persistCurriculum("기본 교육"), "레슨");
		flushAndClear();

		Lesson found = lessonRepository.findWithCategoryById(lesson.getId()).orElseThrow();

		assertThat(Hibernate.isInitialized(found.getCurriculum())).isTrue();
		assertThat(Hibernate.isInitialized(found.getCurriculum().getTopic())).isTrue();
		assertThat(Hibernate.isInitialized(found.getCurriculum().getTopic().getTrainingCategory())).isTrue();
	}

	@Test
	void 없는_레슨을_상위_연관과_함께_조회하면_빈_값을_반환한다() {
		assertThat(lessonRepository.findWithCategoryById(999L)).isEmpty();
	}

	private Curriculum persistCurriculum(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategoryFixture.create(title + " 카테고리", 1, null, null));
		Topic topic = entityManager.persist(TopicFixture.create(category, title, 1, null, null, null));
		return entityManager.persist(CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null));
	}

	private Lesson persistLesson(Curriculum curriculum, String title) {
		return entityManager.persist(LessonFixture.create(curriculum, title, 1, 5));
	}

	private Card persistCard(Lesson lesson, String title, int sortOrder) {
		return entityManager.persist(CardFixture.create(lesson, title, sortOrder, "지시문"));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
