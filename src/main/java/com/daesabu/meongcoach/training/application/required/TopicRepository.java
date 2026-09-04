package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 토픽 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface TopicRepository extends JpaRepository<Topic, Long> {

	/**
	 * 전체 토픽을 카테고리 정렬 순서, 토픽 정렬 순서, id 오름차순으로 조회한다.
	 */
	List<Topic> findAllByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

	/**
	 * 카테고리 정렬 순서, 토픽 정렬 순서, id 오름차순 기준 첫 토픽을 조회한다.
	 */
	Optional<Topic> findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc();

	List<Topic> findAllByOrderBySortOrderAsc();
}
