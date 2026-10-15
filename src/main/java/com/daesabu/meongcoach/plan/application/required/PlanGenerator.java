package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.application.PlanGenerationContext;
import com.daesabu.meongcoach.plan.domain.PlanDraft;

public interface PlanGenerator {

	/**
	 * 컨텍스트로 플랜 초안을 생성한다. retryFeedback이 비어 있지 않으면 프롬프트 끝에 덧붙인다.
	 * 호출·해석 실패 시 PlanGenerationFailedException을 던진다.
	 */
	PlanDraft generate(PlanGenerationContext context, String retryFeedback);
}
