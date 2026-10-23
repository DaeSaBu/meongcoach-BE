package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CustomPlanPreparationResult;
import java.util.Collection;

public interface CustomPlanPreparationFinder {

	/**
	 * 선택한 토픽에 속한 사용자의 미완료 커리큘럼 ID를 교육 순서대로 조회한다.
	 * 요청한 토픽이 하나라도 없으면 TopicNotFoundException을 던진다.
	 */
	CustomPlanPreparationResult find(Long userId, Collection<Long> topicIds);
}
