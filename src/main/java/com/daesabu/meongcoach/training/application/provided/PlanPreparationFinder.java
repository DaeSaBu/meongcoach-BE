package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.PlanPreparationResult;
import java.util.Collection;
import java.util.List;

public interface PlanPreparationFinder {

	/**
	 * 선택한 토픽을 교육 순서로 합쳐 완료 여부와 관계없이 모든 커리큘럼 ID를 조회한다.
	 * 레슨이 없는 커리큘럼이 있으면 IllegalStateException을 던진다.
	 * 요청한 토픽이 하나라도 없으면 TopicNotFoundException을 던진다.
	 */
	PlanPreparationResult find(Collection<Long> topicIds);

	/**
	 * 선택한 토픽을 요청한 순서로 합쳐 완료 여부와 관계없이 모든 커리큘럼 ID를 조회한다.
	 * 토픽 안의 커리큘럼은 교육 순서로 합친다.
	 * 레슨이 없는 커리큘럼이 있으면 IllegalStateException을 던진다.
	 * 요청한 토픽이 하나라도 없으면 TopicNotFoundException을 던진다.
	 */
	PlanPreparationResult findInRequestOrder(List<Long> topicIds);
}
