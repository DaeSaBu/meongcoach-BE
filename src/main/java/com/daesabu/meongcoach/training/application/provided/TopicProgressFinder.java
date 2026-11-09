package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import java.util.List;

public interface TopicProgressFinder {

	/**
	 * 레슨이 있는 모든 토픽을 교육 순서로 반환한다.
	 * 토픽마다 카테고리, 전체 레슨 수, 회원이 완료한 레슨 수, 마지막으로 완료한 시각을 함께 반환한다.
	 * 다음 커리큘럼은 레슨을 모두 완료하지 않은 첫 커리큘럼이며, 모두 완료했으면 첫 커리큘럼이다.
	 * 완료한 레슨이 없으면 마지막 학습 시각은 null이다.
	 */
	List<TopicProgressResult> findAll(Long userId);
}
