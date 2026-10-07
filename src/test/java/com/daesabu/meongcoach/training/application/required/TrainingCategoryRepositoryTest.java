package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class TrainingCategoryRepositoryTest {

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 카테고리를_정렬_순서_오름차순으로_조회한다() {
		persistCategory("생활 습관", 3);
		persistCategory("기본 교육", 1);
		persistCategory("문제 행동", 2);
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAll();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("기본 교육", "문제 행동", "생활 습관");
	}

	@Test
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		TrainingCategory first = persistCategory("먼저 등록", 1);
		TrainingCategory second = persistCategory("나중 등록", 1);
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAll();

		assertThat(categories).extracting(TrainingCategory::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 카테고리의_토픽을_정렬_순서_id_오름차순으로_함께_조회한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic second = persistTopic(category, "둘째", 2);
		Topic firstRegistered = persistTopic(category, "첫째-먼저", 1);
		Topic laterRegistered = persistTopic(category, "첫째-나중", 1);
		entityManager.flush();
		entityManager.clear();

		List<TrainingCategory> categories = trainingCategoryRepository.findAll();

		assertThat(categories).hasSize(1);
		assertThat(categories.getFirst().getTopics()).extracting(Topic::getId)
				.containsExactly(firstRegistered.getId(), laterRegistered.getId(), second.getId());
	}

	@Test
	void 토픽이_없는_카테고리도_조회한다() {
		persistCategory("토픽 없는 카테고리", 1);
		TrainingCategory other = persistCategory("토픽 있는 카테고리", 2);
		persistTopic(other, "앉아", 1);
		entityManager.flush();
		entityManager.clear();

		List<TrainingCategory> categories = trainingCategoryRepository.findAll();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("토픽 없는 카테고리", "토픽 있는 카테고리");
		assertThat(categories.getFirst().getTopics()).isEmpty();
	}

	@Test
	void 등록된_카테고리가_없으면_빈_목록을_반환한다() {
		List<TrainingCategory> categories = trainingCategoryRepository.findAll();

		assertThat(categories).isEmpty();
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(TopicFixture.create(category, title, sortOrder, null, null, null));
	}
}
