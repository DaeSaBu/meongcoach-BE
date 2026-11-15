package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.training.application.required.dto.AiPlanGenerateResult;

public interface AiPlanGenerator {

	/**
	 * 강아지 프로필, 생성일, 사용자 기대사항과 커리큘럼 후보로 AI 플랜 구성을 생성한다.
	 * 기대사항에 직접 맞는 토픽 ID 목록과 목표 문장을 반환한다.
	 * 토픽 ID 목록은 후보에 있는 서로 다른 토픽으로 이루어지며, 목록의 순서가 플랜 진행 순서가 된다.
	 * 응답을 해석할 수 없거나 토픽 ID 목록 또는 목표 문장을 쓸 수 없으면 InvalidAiPlanResponseException을 던진다.
	 */
	AiPlanGenerateResult generate(AiPlanGenerateRequest request);
}
