package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.AiPlanPreparationResult;

public interface AiPlanPreparationFinder {

	/**
	 * 완료 여부와 관계없이 모든 커리큘럼의 정보와 사용자의 학습 현황을 교육 순서로 조회한다.
	 * 커리큘럼이 없으면 빈 목록을 반환하고, 레슨이 없는 커리큘럼이 있으면 IllegalStateException을 던진다.
	 */
	AiPlanPreparationResult find(Long userId);
}
