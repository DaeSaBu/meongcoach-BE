package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 교육 카테고리 조회 리포지토리 검증.
 */
@DataJpaTest
class TrainingCategoryRepositoryTest {

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 카테고리를_정렬_순서_오름차순으로_조회한다() {
		entityManager.persist(TrainingCategory.create("생활 습관", 3, null, null));
		entityManager.persist(TrainingCategory.create("기본 교육", 1, null, null));
		entityManager.persist(TrainingCategory.create("문제 행동", 2, null, null));
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).extracting(TrainingCategory::getTitle)
				.containsExactly("기본 교육", "문제 행동", "생활 습관");
	}

	@Test
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		TrainingCategory first = entityManager.persist(TrainingCategory.create("먼저 등록", 1, null, null));
		TrainingCategory second = entityManager.persist(TrainingCategory.create("나중 등록", 1, null, null));
		entityManager.flush();

		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).extracting(TrainingCategory::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 등록된_카테고리가_없으면_빈_목록을_반환한다() {
		List<TrainingCategory> categories = trainingCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

		assertThat(categories).isEmpty();
	}
}
