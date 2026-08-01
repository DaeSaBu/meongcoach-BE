package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.application.provided.TopicView;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryView;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 교육 카테고리 조회 서비스 검증.
 */
@DataJpaTest
@Import(TrainingCategoryQueryService.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@DisplayName("교육 카테고리 조회 서비스")
class TrainingCategoryQueryServiceTest {

	@Autowired
	private TrainingCategoryFinder trainingCategoryFinder;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	@DisplayName("카테고리를 정렬 순서 오름차순으로 반환한다")
	void findAllOrdersCategoriesBySortOrder() {
		persistCategory("나중 카테고리", 2);
		persistCategory("먼저 카테고리", 1);
		flushAndClear();

		List<TrainingCategoryView> categories = trainingCategoryFinder.findAll();

		assertThat(categories).extracting(TrainingCategoryView::title)
				.containsExactly("먼저 카테고리", "나중 카테고리");
	}

	@Test
	@DisplayName("카테고리 안의 토픽을 정렬 순서 오름차순으로 반환한다")
	void findAllOrdersTopicsBySortOrderWithinCategory() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		persistTopic(category, "셋째 토픽", 3);
		persistTopic(category, "첫째 토픽", 1);
		persistTopic(category, "둘째 토픽", 2);
		flushAndClear();

		List<TrainingCategoryView> categories = trainingCategoryFinder.findAll();

		assertThat(categories).hasSize(1);
		assertThat(categories.getFirst().topics()).extracting(TopicView::title)
				.containsExactly("첫째 토픽", "둘째 토픽", "셋째 토픽");
	}

	@Test
	@DisplayName("토픽을 소속 카테고리에 담아 반환한다")
	void findAllGroupsTopicsIntoOwningCategory() {
		TrainingCategory basic = persistCategory("기본 교육", 1);
		TrainingCategory advanced = persistCategory("심화 교육", 2);
		persistTopic(basic, "앉아", 1);
		persistTopic(advanced, "기다려", 1);
		persistTopic(advanced, "이리와", 2);
		flushAndClear();

		List<TrainingCategoryView> categories = trainingCategoryFinder.findAll();

		assertThat(categories).extracting(TrainingCategoryView::title)
				.containsExactly("기본 교육", "심화 교육");
		assertThat(categories.get(0).topics()).extracting(TopicView::title).containsExactly("앉아");
		assertThat(categories.get(1).topics()).extracting(TopicView::title).containsExactly("기다려", "이리와");
	}

	@Test
	@DisplayName("카테고리와 토픽의 설명 및 아이콘 정보를 반환한다")
	void findAllReturnsDescriptionsAndIconUrls() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(
				"기본 교육", 1, "기본기를 배우는 교육", "https://example.com/basic.png"
		));
		entityManager.persist(Topic.create(category, new TopicCreateCommand(
				"앉아",
				1,
				"앉아 자세를 배우는 훈련",
				"차분히 앉는 방법을 익혀요",
				"https://example.com/sit.png"
		)));
		flushAndClear();

		TrainingCategoryView categoryView = trainingCategoryFinder.findAll().getFirst();

		assertThat(categoryView.description()).isEqualTo("기본기를 배우는 교육");
		assertThat(categoryView.iconUrl()).isEqualTo("https://example.com/basic.png");
		assertThat(categoryView.topics().getFirst())
				.extracting(TopicView::description, TopicView::detail, TopicView::iconUrl)
				.containsExactly(
						"앉아 자세를 배우는 훈련",
						"차분히 앉는 방법을 익혀요",
						"https://example.com/sit.png"
				);
	}

	@Test
	@DisplayName("토픽이 없는 카테고리는 빈 토픽 목록을 갖는다")
	void findAllReturnsEmptyTopicsWhenCategoryHasNoTopic() {
		persistCategory("토픽 없는 카테고리", 1);
		TrainingCategory other = persistCategory("토픽 있는 카테고리", 2);
		persistTopic(other, "앉아", 1);
		flushAndClear();

		List<TrainingCategoryView> categories = trainingCategoryFinder.findAll();

		assertThat(categories).hasSize(2);
		assertThat(categories.getFirst().topics()).isEmpty();
	}

	@Test
	@DisplayName("등록된 카테고리가 없으면 빈 목록을 반환한다")
	void findAllReturnsEmptyListWhenNoCategoryExists() {
		List<TrainingCategoryView> categories = trainingCategoryFinder.findAll();

		assertThat(categories).isEmpty();
	}

	@Test
	@DisplayName("카테고리 수와 무관하게 두 번의 쿼리로 조회한다")
	void findAllExecutesTwoQueries() {
		TrainingCategory basic = persistCategory("기본 교육", 1);
		TrainingCategory advanced = persistCategory("심화 교육", 2);
		persistTopic(basic, "앉아", 1);
		persistTopic(advanced, "기다려", 1);
		flushAndClear();
		Statistics statistics = clearedStatistics();

		trainingCategoryFinder.findAll();

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategory.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder, null, null, null)));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private Statistics clearedStatistics() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		return statistics;
	}
}
