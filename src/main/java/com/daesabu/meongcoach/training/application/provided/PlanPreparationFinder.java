package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.PlanPreparationResult;
import java.util.Collection;

public interface PlanPreparationFinder {

	/**
	 * 선택한 토픽을 교육 순서로 합쳐 첫 미완료 커리큘럼부터 이후의 모든 커리큘럼 ID를 조회한다.
	 * 모두 완료했으면 빈 목록을 반환한다. 레슨이 없는 커리큘럼이 있으면 IllegalStateException을 던진다.
	 * 요청한 토픽이 하나라도 없으면 TopicNotFoundException을 던진다.
	 */
	PlanPreparationResult find(Long userId, Collection<Long> topicIds);
}
