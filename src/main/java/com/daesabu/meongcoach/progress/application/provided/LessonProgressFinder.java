package com.daesabu.meongcoach.progress.application.provided;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 레슨 진행도 조회 능력. 다른 모듈은 이 인터페이스로만 진행도를 읽는다.
 */
public interface LessonProgressFinder {

	/**
	 * 주어진 레슨 중 한 번이라도 완료한 레슨의 id를 반환한다.
	 */
	Set<Long> findCompletedLessonIds(Long userId, Collection<Long> lessonIds);

	/**
	 * 주어진 레슨의 반복 완료 횟수를 반환한다. 진행 기록이 없는 레슨은 0으로 채운다.
	 */
	Map<Long, Integer> findCompletedCounts(Long userId, Collection<Long> lessonIds);
}
