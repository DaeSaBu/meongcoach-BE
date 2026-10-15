package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;

/**
 * 맞춤 플랜 조회 능력.
 */
public interface PlanFinder {

	/**
	 * 사용자의 진행 중인 플랜 상세와 완료한 플랜 이력 여부를 조회한다.
	 * 진행 중인 플랜이 없거나 그 플랜의 강아지가 삭제됐으면 플랜은 null이다.
	 */
	CurrentPlanResult findCurrent(Long userId);
}
