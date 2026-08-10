package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
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
		entityManager.persist(Topic.create(category, new TopicCreateCommand("산책 훈련", 2, "즐겁고 안전한 첫 산책", null, null)));
		entityManager.persist(Topic.create(category, new TopicCreateCommand("배변 훈련", 1, "편안한 배변 습관 만들기", null, null)));

		List<TopicSummary> topics = service.findAllOrdered();

		assertThat(topics).extracting(TopicSummary::title)
				.containsExactly("배변 훈련", "산책 훈련");
		assertThat(topics).extracting(TopicSummary::description)
				.containsExactly("편안한 배변 습관 만들기", "즐겁고 안전한 첫 산책");
	}

	@Test
	@DisplayName("토픽이 없으면 빈 목록을 반환한다")
	void findAllOrderedReturnsEmptyListWhenNoTopics() {
		assertThat(service.findAllOrdered()).isEmpty();
	}
}
