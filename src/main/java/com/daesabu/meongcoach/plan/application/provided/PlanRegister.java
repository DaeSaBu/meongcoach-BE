package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeRecordRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import jakarta.validation.Valid;

public interface PlanRegister {

	/**
	 * 선택된 강아지와 요청한 토픽의 첫 미완료 커리큘럼부터 이후의 모든 커리큘럼으로 플랜을 생성하고 기존 활성 플랜을 교체한다.
	 */
	Plan createCustom(Long userId, @Valid CustomPlanCreateRequest request);

	/**
	 * 선택된 강아지와 사용자 기대사항으로 AI 플랜을 생성하고, 성공하면 기존 활성 플랜을 교체한다.
	 * 미완료 커리큘럼이 없으면 EmptyPlanCurriculumsException을 던진다.
	 */
	Plan createAiPlan(Long userId, @Valid AiPlanCreateRequest request);

	/**
	 * 사용자가 소유한 완료된 플랜에 회고 결과를 기록하고, 걸린 날과 플랜 커리큘럼이 속한 토픽을 함께 반환한다.
	 * 플랜이 없거나 사용자의 소유가 아니면 {@code PlanNotFoundException}을 던진다.
	 * 회고 결과가 ACHIEVED, PARTIAL, NOT_YET 중 하나가 아니면 {@code InvalidPlanOutcomeException}을 던진다.
	 * 완료되지 않은 플랜이면 {@code PlanNotCompletedException}을 던진다.
	 * 이미 회고 결과를 기록한 플랜이면 {@code PlanOutcomeAlreadyRecordedException}을 던진다.
	 */
	PlanOutcomeResult recordOutcome(Long userId, Long planId, @Valid PlanOutcomeRecordRequest request);
}
