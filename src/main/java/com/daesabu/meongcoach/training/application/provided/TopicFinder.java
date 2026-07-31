package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 토픽 목록 조회 공개 API.
 */
public interface TopicFinder {

	/**
	 * 모든 토픽을 정렬 순서대로 조회한다.
	 */
	List<TopicSummary> findAllOrdered();
}
