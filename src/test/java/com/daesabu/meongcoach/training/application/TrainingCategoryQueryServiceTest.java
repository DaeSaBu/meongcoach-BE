package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.application.provided.TopicResult;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryResult;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
class TrainingCategoryQueryServiceTest {

	@Autowired
	private TrainingCategoryFinder trainingCategoryFinder;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void 카테고리를_정렬_순서_오름차순으로_반환한다() {
		persistCategory("나중 카테고리", 2);
		persistCategory("먼저 카테고리", 1);
		flushAndClear();

		List<TrainingCategoryResult> categories = trainingCategoryFinder.findAll();

		assertThat(categories).extracting(TrainingCategoryResult::title)
				.containsExactly("먼저 카테고리", "나중 카테고리");
	}

	@Test
	void 카테고리_안의_토픽을_정렬_순서_오름차순으로_반환한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		persistTopic(category, "셋째 토픽", 3);
		persistTopic(category, "첫째 토픽", 1);
		persistTopic(category, "둘째 토픽", 2);
		flushAndClear();

		List<TrainingCategoryResult> categories = trainingCategoryFinder.findAll();

		assertThat(categories).hasSize(1);
		assertThat(categories.getFirst().topics()).extracting(TopicResult::title)
				.containsExactly("첫째 토픽", "둘째 토픽", "셋째 토픽");
	}

	@Test
	void 토픽을_소속_카테고리에_담아_반환한다() {
		TrainingCategory basic = persistCategory("기본 교육", 1);
		TrainingCategory advanced = persistCategory("심화 교육", 2);
		persistTopic(basic, "앉아", 1);
		persistTopic(advanced, "기다려", 1);
		persistTopic(advanced, "이리와", 2);
		flushAndClear();

		List<TrainingCategoryResult> categories = trainingCategoryFinder.findAll();

		assertThat(categories).extracting(TrainingCategoryResult::title)
				.containsExactly("기본 교육", "심화 교육");
		assertThat(categories.get(0).topics()).extracting(TopicResult::title).containsExactly("앉아");
		assertThat(categories.get(1).topics()).extracting(TopicResult::title).containsExactly("기다려", "이리와");
	}

	@Test
	void 카테고리와_토픽의_설명_및_아이콘_정보를_반환한다() {
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

		TrainingCategoryResult categoryResult = trainingCategoryFinder.findAll().getFirst();

		assertThat(categoryResult.description()).isEqualTo("기본기를 배우는 교육");
		assertThat(categoryResult.iconUrl()).isEqualTo("https://example.com/basic.png");
		assertThat(categoryResult.topics().getFirst())
				.extracting(TopicResult::description, TopicResult::detail, TopicResult::iconUrl)
				.containsExactly(
						"앉아 자세를 배우는 훈련",
						"차분히 앉는 방법을 익혀요",
						"https://example.com/sit.png"
				);
	}

	@Test
	void 토픽이_없는_카테고리는_빈_토픽_목록을_갖는다() {
		persistCategory("토픽 없는 카테고리", 1);
		TrainingCategory other = persistCategory("토픽 있는 카테고리", 2);
		persistTopic(other, "앉아", 1);
		flushAndClear();

		List<TrainingCategoryResult> categories = trainingCategoryFinder.findAll();

		assertThat(categories).hasSize(2);
		assertThat(categories.getFirst().topics()).isEmpty();
	}

	@Test
	void 등록된_카테고리가_없으면_빈_목록을_반환한다() {
		List<TrainingCategoryResult> categories = trainingCategoryFinder.findAll();

		assertThat(categories).isEmpty();
	}

	@Test
	void 카테고리_수와_무관하게_두_번의_쿼리로_조회한다() {
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
