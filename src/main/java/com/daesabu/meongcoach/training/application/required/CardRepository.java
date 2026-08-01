package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 카드 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface CardRepository extends JpaRepository<Card, Long> {

	/**
	 * 특정 레슨의 카드를 정렬 순서 오름차순으로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<Card> findAllByLesson_IdOrderBySortOrderAscIdAsc(Long lessonId);
}
