package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;

public interface AiPlanGenerator {

	/**
	 * 강아지 프로필, 생성일, 연습 설정, 사용자 기대사항과 커리큘럼 후보로 AI 플랜 구성을 생성한다.
	 * 선택한 토픽 ID 목록과 목표 문장을 반환한다.
	 * 새 훈련 분량은 2주 총 연습 시간의 50%에 더 가까운 구성을 선택하고, 차이가 같으면 적은 분량을 선택한다.
	 * 미완료 훈련이 있으면 목표 시간을 초과하더라도 미완료 커리큘럼을 포함한 토픽을 최소 하나 선택한다.
	 */
	AiPlanGenerateResult generate(AiPlanGenerateRequest request);
}
