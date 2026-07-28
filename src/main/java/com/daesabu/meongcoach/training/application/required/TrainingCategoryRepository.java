package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 교육 카테고리 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface TrainingCategoryRepository extends JpaRepository<TrainingCategory, Long> {

	/**
	 * 전체 카테고리를 정렬 순서 오름차순으로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<TrainingCategory> findAllByOrderBySortOrderAscIdAsc();
}
