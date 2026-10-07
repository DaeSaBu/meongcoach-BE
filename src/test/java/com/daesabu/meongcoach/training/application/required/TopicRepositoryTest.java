package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class TopicRepositoryTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 정렬_순서가_가장_앞선_토픽_하나를_조회한다() {
		TrainingCategory later = persistCategory("나중 카테고리", 2);
		TrainingCategory earlier = persistCategory("먼저 카테고리", 1);
		persistTopic(later, "나중-첫째", 1);
		persistTopic(earlier, "먼저-둘째", 2);
		persistTopic(earlier, "먼저-첫째", 1);
		entityManager.flush();

		Optional<Topic> topic = topicRepository.findFirstTopic();

		assertThat(topic).isPresent();
		assertThat(topic.get().getTitle()).isEqualTo("먼저-첫째");
	}

	@Test
	void 카테고리_정렬_순서가_같으면_id가_앞선_카테고리의_토픽을_첫_토픽으로_조회한다() {
		TrainingCategory first = persistCategory("먼저 등록", 1);
		TrainingCategory second = persistCategory("나중 등록", 1);
		persistTopic(first, "먼저-둘째", 2);
		persistTopic(second, "나중-첫째", 1);
		entityManager.flush();

		Optional<Topic> topic = topicRepository.findFirstTopic();

		assertThat(topic).map(Topic::getTitle).contains("먼저-둘째");
	}

	@Test
	void 첫_토픽을_카테고리와_함께_조회한다() {
		persistTopic(persistCategory("기본 교육", 1), "앉아", 1);
		flushAndClear();

		Topic topic = topicRepository.findFirstTopic().orElseThrow();

		assertThat(Hibernate.isInitialized(topic.getTrainingCategory())).isTrue();
	}

	@Test
	void 토픽을_카테고리와_함께_조회한다() {
		Topic topic = persistTopic(persistCategory("기본 교육", 1), "앉아", 1);
		flushAndClear();

		Topic found = topicRepository.findWithCategoryById(topic.getId()).orElseThrow();

		assertThat(Hibernate.isInitialized(found.getTrainingCategory())).isTrue();
	}

	@Test
	void 없는_토픽을_카테고리와_함께_조회하면_빈_값을_반환한다() {
		assertThat(topicRepository.findWithCategoryById(999L)).isEmpty();
	}

	@Test
	void 등록된_토픽이_없으면_빈_값을_반환한다() {
		Optional<Topic> topic = topicRepository.findFirstTopic();

		assertThat(topic).isEmpty();
	}

	@Test
	void 전체_토픽을_카테고리_정렬_순서_다음_토픽_정렬_순서로_조회한다() {
		TrainingCategory later = persistCategory("나중 카테고리", 2);
		TrainingCategory earlier = persistCategory("먼저 카테고리", 1);
		persistTopic(later, "나중-첫째", 1);
		persistTopic(earlier, "먼저-둘째", 2);
		persistTopic(earlier, "먼저-첫째", 1);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllOrdered();

		assertThat(topics).extracting(Topic::getTitle)
				.containsExactly("먼저-첫째", "먼저-둘째", "나중-첫째");
	}

	@Test
	void 카테고리_정렬_순서가_같으면_id가_앞선_카테고리의_토픽부터_조회한다() {
		TrainingCategory first = persistCategory("먼저 등록", 1);
		TrainingCategory second = persistCategory("나중 등록", 1);
		persistTopic(second, "나중-첫째", 1);
		persistTopic(first, "먼저-둘째", 2);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllOrdered();

		assertThat(topics).extracting(Topic::getTitle)
				.containsExactly("먼저-둘째", "나중-첫째");
	}

	@Test
	void 정렬_순서가_같은_토픽은_id_오름차순으로_조회한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic first = persistTopic(category, "먼저 등록", 1);
		Topic second = persistTopic(category, "나중 등록", 1);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllOrdered();

		assertThat(topics).extracting(Topic::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 토픽의_커리큘럼을_정렬_순서_오름차순으로_로딩하고_다른_토픽의_커리큘럼은_제외한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic topic = persistTopic(category, "앉아", 1);
		Topic other = persistTopic(category, "기다려", 2);
		persistCurriculum(topic, "셋째", 3);
		persistCurriculum(topic, "첫째", 1);
		persistCurriculum(topic, "둘째", 2);
		persistCurriculum(other, "다른 토픽 커리큘럼", 1);
		flushAndClear();

		Topic found = topicRepository.findById(topic.getId()).orElseThrow();

		assertThat(found.getCurriculums()).extracting(Curriculum::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 정렬_순서가_같은_커리큘럼은_id_오름차순으로_로딩한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic topic = persistTopic(category, "앉아", 1);
		Curriculum first = persistCurriculum(topic, "먼저 등록", 1);
		Curriculum second = persistCurriculum(topic, "나중 등록", 1);
		flushAndClear();

		Topic found = topicRepository.findById(topic.getId()).orElseThrow();

		assertThat(found.getCurriculums()).extracting(Curriculum::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 커리큘럼이_없는_토픽은_빈_커리큘럼_목록으로_로딩한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic topic = persistTopic(category, "앉아", 1);
		flushAndClear();

		Topic found = topicRepository.findById(topic.getId()).orElseThrow();

		assertThat(found.getCurriculums()).isEmpty();
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(TopicFixture.create(category, title, sortOrder, null, null, null));
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		return entityManager.persist(CurriculumFixture.create(topic, title, sortOrder, null, null));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
