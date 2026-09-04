package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.CardMedia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 카드 미디어 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface CardMediaRepository extends JpaRepository<CardMedia, Long> {

	/**
	 * 여러 카드의 미디어를 한 번에 조회한다. 정렬 순서 오름차순이며, 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<CardMedia> findAllByCard_IdInOrderBySortOrderAscIdAsc(Collection<Long> cardIds);
}
