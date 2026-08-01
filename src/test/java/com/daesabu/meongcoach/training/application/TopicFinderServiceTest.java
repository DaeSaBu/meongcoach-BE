package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("토픽 조회 서비스")
class TopicFinderServiceTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TestEntityManager entityManager;

	private TopicFinderService service;

	@BeforeEach
	void setUp() {
		service = new TopicFinderService(topicRepository);
	}

	@Test
	@DisplayName("토픽을 정렬 순서대로 조회한다")
	void findAllOrderedReturnsTopicsSortedBySortOrder() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 훈련", 1, null, null));
		entityManager.persist(Topic.create(category, new TopicCreateCommand("산책 훈련", 2, null, null, null)));
		entityManager.persist(Topic.create(category, new TopicCreateCommand("배변 훈련", 1, null, null, null)));

		List<TopicSummary> topics = service.findAllOrdered();

		assertThat(topics).extracting(TopicSummary::title)
				.containsExactly("배변 훈련", "산책 훈련");
	}

	@Test
	@DisplayName("토픽이 없으면 빈 목록을 반환한다")
	void findAllOrderedReturnsEmptyListWhenNoTopics() {
		assertThat(service.findAllOrdered()).isEmpty();
	}

	@Test
	@DisplayName("모든 토픽 ID가 존재하면 검증을 통과한다")
	void validateAllExistPassesWhenAllTopicsExist() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 훈련", 1, null, null));
		Topic first = entityManager.persist(
				Topic.create(category, new TopicCreateCommand("배변 훈련", 1, null, null, null)));
		Topic second = entityManager.persist(
				Topic.create(category, new TopicCreateCommand("산책 훈련", 2, null, null, null)));

		assertThatCode(() -> service.validateAllExist(Set.of(first.getId(), second.getId())))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("존재하지 않는 토픽 ID가 있으면 검증에 실패한다")
	void validateAllExistFailsWhenTopicDoesNotExist() {
		assertThatThrownBy(() -> service.validateAllExist(Set.of(999L)))
				.isInstanceOf(TopicNotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	@DisplayName("토픽 ID가 비어 있으면 조회 없이 검증을 통과한다")
	void validateAllExistPassesWhenTopicIdsAreEmpty() {
		assertThatCode(() -> service.validateAllExist(Set.of()))
				.doesNotThrowAnyException();
	}
}
