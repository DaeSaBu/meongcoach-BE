package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import jakarta.validation.Valid;

/**
 * 맞춤 플랜 마무리 능력.
 */
public interface PlanCompleter {

	/**
	 * 진행 중인 플랜을 결과와 함께 마무리하고 진행 집계, 걸린 일수, 반영한 토픽을 반환한다.
	 * 진행률이 100%가 아니어도 마무리할 수 있으며, 달성(ACHIEVED)과 일부 달성(PARTIAL)이면
	 * 플랜의 토픽을 사용자가 배운 토픽에 누적한다.
	 * 본인의 플랜이 아니면 {@code PlanNotFoundException}, 진행 중이 아니면 {@code PlanNotActiveException}을 던진다.
	 */
	PlanCompletionResult complete(Long userId, Long planId, @Valid PlanCompleteRequest request);
}
