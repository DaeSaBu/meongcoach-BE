package com.daesabu.meongcoach.progress.application.provided;

import java.util.Optional;

/**
 * 토픽 진입 기록 조회 능력. 다른 모듈은 이 인터페이스로만 진입 기록을 읽는다.
 */
public interface TopicEntryFinder {

	/**
	 * 한 사용자가 가장 최근에 진입한 토픽의 id를 반환한다. 진입 기록이 없으면 빈 Optional을 반환한다.
	 */
	Optional<Long> findLatestEnteredTopicId(Long userId);
}
