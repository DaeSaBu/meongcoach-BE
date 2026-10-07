package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TopicsResult;
import java.util.List;

/**
 * 토픽 목록 조회 공개 API.
 */
public interface TopicFinder {

	/**
	 * 모든 토픽을 카테고리 정렬 순서, 토픽 정렬 순서대로 조회한다. 정렬 순서가 같으면 id 오름차순이다.
	 */
	List<TopicsResult> findAll();
}
