package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import java.util.Optional;

public interface CurrentPlanFinder {

	/**
	 * 사용자의 활성 플랜을 우선 반환하고, 없으면 가장 최근 완료한 플랜을 반환한다.
	 * 두 상태의 플랜이 모두 없으면 빈 결과를 반환한다.
	 */
	Optional<CurrentPlanResult> find(Long userId);
}
