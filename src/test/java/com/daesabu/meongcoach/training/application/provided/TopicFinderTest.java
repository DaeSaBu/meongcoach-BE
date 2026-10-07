package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TopicFinderTest {

	@Autowired
	private TopicFinder topicFinder;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 토픽을_정렬_순서대로_조회한다() {
		TrainingCategory category = saveCategory("기본 훈련", 1);
		topicRepository.save(TopicFixture.create(category, "산책 훈련", 2, "즐겁고 안전한 첫 산책", null, null));
		topicRepository.save(TopicFixture.create(category, "배변 훈련", 1, "편안한 배변 습관 만들기", null, null));
		flushAndClear();

		List<TopicResult> topics = topicFinder.findAllOrdered();

		assertThat(topics).extracting(TopicResult::title)
				.containsExactly("배변 훈련", "산책 훈련");
		assertThat(topics).extracting(TopicResult::description)
				.containsExactly("편안한 배변 습관 만들기", "즐겁고 안전한 첫 산책");
	}

	@Test
	void 카테고리_정렬_순서가_토픽_정렬_순서보다_우선한다() {
		TrainingCategory later = saveCategory("나중 카테고리", 2);
		TrainingCategory earlier = saveCategory("먼저 카테고리", 1);
		topicRepository.save(TopicFixture.create(later, "나중-첫째", 1, null, null, null));
		topicRepository.save(TopicFixture.create(earlier, "먼저-둘째", 2, null, null, null));
		topicRepository.save(TopicFixture.create(earlier, "먼저-첫째", 1, null, null, null));
		flushAndClear();

		List<TopicResult> topics = topicFinder.findAllOrdered();

		assertThat(topics).extracting(TopicResult::title)
				.containsExactly("먼저-첫째", "먼저-둘째", "나중-첫째");
	}

	@Test
	void 토픽이_없으면_빈_목록을_반환한다() {
		assertThat(topicFinder.findAllOrdered()).isEmpty();
	}

	private TrainingCategory saveCategory(String title, int sortOrder) {
		return trainingCategoryRepository.save(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
