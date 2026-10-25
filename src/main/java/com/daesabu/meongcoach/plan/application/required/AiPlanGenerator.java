package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;

public interface AiPlanGenerator {

	/**
	 * 강아지 프로필, 생성일, 연습 설정, 사용자 기대사항과 커리큘럼 후보로 AI 플랜 구성을 생성한다.
	 * 선택한 토픽 ID 목록과 목표 문장을 반환한다.
	 */
	AiPlanGenerateResult generate(AiPlanGenerateRequest request);
}
