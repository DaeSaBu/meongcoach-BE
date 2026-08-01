package com.daesabu.meongcoach.training.application.provided;

import java.util.Set;

/**
 * 다른 모듈에서 받은 토픽 ID의 유효성을 검증하는 공개 API.
 */
public interface TopicValidator {

	/**
	 * 모든 토픽 ID가 존재하는지 검증한다. 빈 집합은 선택하지 않은 상태이므로 통과한다.
	 */
	void validateAllExist(Set<Long> topicIds);
}
