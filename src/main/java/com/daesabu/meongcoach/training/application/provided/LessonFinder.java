package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 레슨 카드 조회 능력. 레슨 시작 화면을 구성한다.
 */
public interface LessonFinder {

	/**
	 * 레슨의 카드를 정렬 순서 오름차순으로 조회한다. 각 카드의 미디어도 정렬 순서 오름차순이다.
	 *
	 * @throws com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException 레슨이 없으면 발생한다
	 */
	List<CardResult> findCards(Long lessonId);
}
