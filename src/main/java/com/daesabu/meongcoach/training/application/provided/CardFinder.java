package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import java.util.Collection;
import java.util.List;

/**
 * 카드 요약 조회 능력.
 */
public interface CardFinder {

	/**
	 * 카드 ID 목록에 해당하는 카드 요약을 반환한다. 요약에는 카드가 속한 훈련 카테고리·토픽·커리큘럼·레슨의 ID와 제목이 담긴다.
	 * 존재하지 않는 ID는 결과에서 빠진다.
	 */
	List<CardSummaryResult> findSummaries(Collection<Long> cardIds);
}
