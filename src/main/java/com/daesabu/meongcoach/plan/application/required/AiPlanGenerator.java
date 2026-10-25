package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;

public interface AiPlanGenerator {

	/**
	 * 강아지 프로필, 생성일, 연습 설정, 사용자 기대사항과 커리큘럼 후보로 AI 플랜 구성을 생성한다.
	 * 선택한 토픽 ID 목록과 목표 문장을 반환한다.
	 * 2주 총 연습 시간의 50%를 새 훈련 분량 기준으로 사용하고, 나머지 50%는 반복 연습 시간으로 남긴다.
	 */
	AiPlanGenerateResult generate(AiPlanGenerateRequest request);
}
