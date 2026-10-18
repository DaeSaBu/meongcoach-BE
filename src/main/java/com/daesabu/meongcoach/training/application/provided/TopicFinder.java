package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.Collection;
import java.util.List;

/**
 * 토픽 목록 조회 공개 API.
 */
public interface TopicFinder {

	/**
	 * 모든 토픽을 카테고리 정렬 순서, 토픽 정렬 순서대로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<TopicResult> findAllOrdered();

	/**
	 * 토픽 여러 개를 id 목록으로 조회한다. 없는 id는 결과에서 빠지며 순서는 보장하지 않는다.
	 */
	List<TopicResult> findTopics(Collection<Long> topicIds);

	/**
	 * 토픽 하나를 id로 조회한다. 없으면 TopicNotFoundException을 던진다.
	 */
	TopicResult findTopic(Long topicId);
}
