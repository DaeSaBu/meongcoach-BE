package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

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
class CurriculumRepositoryTest {

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 커리큘럼의_레슨을_정렬_순서_오름차순으로_로딩하고_다른_커리큘럼의_레슨은_제외한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "다른 커리큘럼", 2);
		persistLesson(curriculum, "셋째", 3);
		persistLesson(curriculum, "첫째", 1);
		persistLesson(curriculum, "둘째", 2);
		persistLesson(other, "다른 레슨", 1);
		flushAndClear();

		Curriculum found = curriculumRepository.findById(curriculum.getId()).orElseThrow();

		assertThat(found.getLessons()).extracting(Lesson::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 정렬_순서가_같은_레슨은_id_오름차순으로_로딩한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		Lesson first = persistLesson(curriculum, "먼저 등록", 1);
		Lesson second = persistLesson(curriculum, "나중 등록", 1);
		flushAndClear();

		Curriculum found = curriculumRepository.findById(curriculum.getId()).orElseThrow();

		assertThat(found.getLessons()).extracting(Lesson::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 레슨이_없는_커리큘럼은_빈_레슨_목록으로_로딩한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "레슨 없는 커리큘럼", 1);
		flushAndClear();

		Curriculum found = curriculumRepository.findById(curriculum.getId()).orElseThrow();

		assertThat(found.getLessons()).isEmpty();
	}

	@Test
	void 커리큘럼을_토픽_카테고리와_함께_조회한다() {
		Curriculum curriculum = persistCurriculum(persistTopic("기본 교육"), "커리큘럼", 1);
		flushAndClear();

		Curriculum found = curriculumRepository.findWithCategoryById(curriculum.getId()).orElseThrow();

		assertThat(Hibernate.isInitialized(found.getTopic())).isTrue();
		assertThat(Hibernate.isInitialized(found.getTopic().getTrainingCategory())).isTrue();
	}

	@Test
	void 없는_커리큘럼을_상위_연관과_함께_조회하면_빈_값을_반환한다() {
		assertThat(curriculumRepository.findWithCategoryById(999L)).isEmpty();
	}

	private Topic persistTopic(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategoryFixture.create(title + " 카테고리", 1, null, null));
		return entityManager.persist(TopicFixture.create(category, title, 1, null, null, null));
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		return entityManager.persist(CurriculumFixture.create(topic, title, sortOrder, null, null));
	}

	private Lesson persistLesson(Curriculum curriculum, String title, int sortOrder) {
		return entityManager.persist(LessonFixture.create(curriculum, title, sortOrder, 5));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
