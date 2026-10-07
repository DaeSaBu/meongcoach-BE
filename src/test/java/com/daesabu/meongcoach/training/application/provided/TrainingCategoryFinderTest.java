package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TrainingCategoryFinderTest {

	@Autowired
	private TrainingCategoryFinder trainingCategoryFinder;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 카테고리를_정렬_순서_오름차순으로_반환한다() {
		saveCategory("나중 카테고리", 2);
		saveCategory("먼저 카테고리", 1);
		flushAndClear();

		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("먼저 카테고리", "나중 카테고리");
	}

	@Test
	void 카테고리_안의_토픽을_정렬_순서_오름차순으로_반환한다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		saveTopic(category, "셋째 토픽", 3);
		saveTopic(category, "첫째 토픽", 1);
		saveTopic(category, "둘째 토픽", 2);
		flushAndClear();

		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();

		assertThat(categories).hasSize(1);
		assertThat(categories.getFirst().getTopics()).extracting(Topic::getTitle)
				.containsExactly("첫째 토픽", "둘째 토픽", "셋째 토픽");
	}

	@Test
	void 토픽을_소속_카테고리에_담아_반환한다() {
		TrainingCategory basic = saveCategory("기본 교육", 1);
		TrainingCategory advanced = saveCategory("심화 교육", 2);
		saveTopic(basic, "앉아", 1);
		saveTopic(advanced, "기다려", 1);
		saveTopic(advanced, "이리와", 2);
		flushAndClear();

		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("기본 교육", "심화 교육");
		assertThat(categories.get(0).getTopics()).extracting(Topic::getTitle).containsExactly("앉아");
		assertThat(categories.get(1).getTopics()).extracting(Topic::getTitle).containsExactly("기다려", "이리와");
	}

	@Test
	void 카테고리와_토픽의_설명_및_아이콘_정보를_반환한다() {
		TrainingCategory category = trainingCategoryRepository.save(TrainingCategoryFixture.create(
				"기본 교육", 1, "기본기를 배우는 교육", "https://example.com/basic.png"
		));
		topicRepository.save(TopicFixture.create(
				category,
				"앉아",
				1,
				"앉아 자세를 배우는 훈련",
				"차분히 앉는 방법을 익혀요",
				"https://example.com/sit.png"
		));
		flushAndClear();

		TrainingCategory found = trainingCategoryFinder.findAllOrdered().getFirst();

		assertThat(found.getDescription()).isEqualTo("기본기를 배우는 교육");
		assertThat(found.getIconUrl()).isEqualTo("https://example.com/basic.png");
		assertThat(found.getTopics().getFirst())
				.extracting(Topic::getDescription, Topic::getDetail, Topic::getIconUrl)
				.containsExactly(
						"앉아 자세를 배우는 훈련",
						"차분히 앉는 방법을 익혀요",
						"https://example.com/sit.png"
				);
	}

	@Test
	void 토픽이_없는_카테고리는_빈_토픽_목록을_갖는다() {
		saveCategory("토픽 없는 카테고리", 1);
		TrainingCategory other = saveCategory("토픽 있는 카테고리", 2);
		saveTopic(other, "앉아", 1);
		flushAndClear();

		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();

		assertThat(categories).hasSize(2);
		assertThat(categories.getFirst().getTopics()).isEmpty();
	}

	@Test
	void 등록된_카테고리가_없으면_빈_목록을_반환한다() {
		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();

		assertThat(categories).isEmpty();
	}

	private TrainingCategory saveCategory(String title, int sortOrder) {
		return trainingCategoryRepository.save(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private Topic saveTopic(TrainingCategory category, String title, int sortOrder) {
		return topicRepository.save(TopicFixture.create(category, title, sortOrder, null, null, null));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
