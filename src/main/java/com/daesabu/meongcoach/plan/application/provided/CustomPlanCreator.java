package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.Plan;
import jakarta.validation.Valid;

public interface CustomPlanCreator {

	/**
	 * 선택된 강아지와 요청한 토픽의 미완료 커리큘럼으로 플랜을 생성하고 기존 활성 플랜을 교체한다.
	 */
	Plan create(Long userId, @Valid CustomPlanCreateRequest request);
}
