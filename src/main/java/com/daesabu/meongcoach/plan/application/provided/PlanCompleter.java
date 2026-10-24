package com.daesabu.meongcoach.plan.application.provided;

public interface PlanCompleter {

	/**
	 * 회원의 활성 플랜에 포함된 모든 커리큘럼이 완료됐으면 플랜을 완료한다.
	 * 활성 플랜이 없거나 미완료 커리큘럼이 있으면 변경하지 않는다.
	 */
	void complete(Long userId);
}
