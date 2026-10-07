package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;

public interface LessonFinder {

	/**
	 * 레슨의 카드를 조회한다.
	 * 이용권이 필요한 유료 커리큘럼의 레슨을 사용자가 이용권 없이 조회하면 {@code EntitlementRequiredException}을 던진다.
	 */
	List<Card> findCards(Long userId, Long lessonId);
}
