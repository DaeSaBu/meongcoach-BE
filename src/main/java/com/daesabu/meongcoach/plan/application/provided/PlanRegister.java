package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
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
}
