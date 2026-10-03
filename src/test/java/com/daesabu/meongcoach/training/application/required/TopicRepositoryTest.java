package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import java.util.Optional;
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
	void 등록된_토픽이_없으면_빈_값을_반환한다() {
		Optional<Topic> topic = topicRepository.findFirstTopic();

		assertThat(topic).isEmpty();
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(TopicFixture.create(category, title, sortOrder, null, null, null));
	}
}
