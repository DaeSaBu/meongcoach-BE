package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 토픽 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("토픽 리포지토리")
class TopicRepositoryTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("카테고리 정렬 순서, 토픽 정렬 순서 오름차순으로 조회한다")
	void findAllOrdersByCategorySortOrderThenTopicSortOrder() {
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
	@DisplayName("카테고리와 정렬 순서가 같으면 id 오름차순으로 조회한다")
	void findAllOrdersByIdAscendingWhenSortOrderIsSame() {
		TrainingCategory category = persistCategory("기본 훈련", 1);
		Topic first = persistTopic(category, "먼저 등록", 1);
		Topic second = persistTopic(category, "나중 등록", 1);
		entityManager.flush();

		List<Topic> topics = topicRepository.findAllByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topics).extracting(Topic::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("정렬 순서가 가장 앞선 토픽 하나를 조회한다")
	void findFirstReturnsTopicWithLowestSortOrder() {
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
	@DisplayName("등록된 토픽이 없으면 빈 값을 반환한다")
	void findFirstReturnsEmptyWhenNoTopicExists() {
		Optional<Topic> topic = topicRepository.findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

		assertThat(topic).isEmpty();
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategory.create(title, sortOrder));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder)));
	}
}
