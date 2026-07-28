package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 교육 카테고리 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("교육 카테고리 리포지토리")
class TrainingCategoryRepositoryTest {

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("카테고리를 정렬 순서 오름차순으로 조회한다")
	void findAllOrdersBySortOrderAscending() {
		entityManager.persist(TrainingCategory.create("생활 습관", 3));
		entityManager.persist(TrainingCategory.create("기본 교육", 1));
		entityManager.persist(TrainingCategory.create("문제 행동", 2));
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("기본 교육", "문제 행동", "생활 습관");
	}

	@Test
	@DisplayName("정렬 순서가 같으면 id 오름차순으로 조회한다")
	void findAllOrdersByIdAscendingWhenSortOrderIsSame() {
		TrainingCategory first = entityManager.persist(TrainingCategory.create("먼저 등록", 1));
		TrainingCategory second = entityManager.persist(TrainingCategory.create("나중 등록", 1));
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).extracting(TrainingCategory::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("등록된 카테고리가 없으면 빈 목록을 반환한다")
	void findAllReturnsEmptyListWhenNoCategoryExists() {
		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).isEmpty();
	}
}
