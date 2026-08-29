package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 토픽 조회 리포지토리 검증.
 */
@DataJpaTest
class TopicRepositoryTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 카테고리_정렬_순서_토픽_정렬_순서_오름차순으로_조회한다() {
		TrainingCategory later = persistCategory("나중 카테고리", 2);
		TrainingCategory earlier = persistCategory("먼저 카테고리", 1);
		persistTopic(later, "나중-첫째", 1);
		persistTopic(earlier, "먼저-둘째", 2);
		persistTopic(earlier, "먼저-첫째", 1);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topics).extracting(Topic::getTitle)
				.containsExactly("먼저-첫째", "먼저-둘째", "나중-첫째");
	}

	@Test
	void 카테고리와_정렬_순서가_같으면_id_오름차순으로_조회한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic first = persistTopic(category, "먼저 등록", 1);
		Topic second = persistTopic(category, "나중 등록", 1);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topics).extracting(Topic::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 정렬_순서가_가장_앞선_토픽_하나를_조회한다() {
		TrainingCategory later = persistCategory("나중 카테고리", 2);
		TrainingCategory earlier = persistCategory("먼저 카테고리", 1);
		persistTopic(later, "나중-첫째", 1);
		persistTopic(earlier, "먼저-둘째", 2);
		persistTopic(earlier, "먼저-첫째", 1);
		entityManager.flush();

		Optional<Topic> topic = topicRepository.findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topic).isPresent();
		assertThat(topic.get().getTitle()).isEqualTo("먼저-첫째");
	}

	@Test
	void 등록된_토픽이_없으면_빈_값을_반환한다() {
		Optional<Topic> topic = topicRepository.findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topic).isEmpty();
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategory.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder, null, null, null)));
	}
}
