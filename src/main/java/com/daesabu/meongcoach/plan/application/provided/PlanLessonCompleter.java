package com.daesabu.meongcoach.plan.application.provided;

public interface PlanLessonCompleter {

	/**
	 * 사용자가 소유한 활성 플랜에서 현재 진행할 레슨을 완료한다.
	 * 일반 학습 기록과 플랜의 완료 기록을 반영하고, 모든 레슨을 완료하면 플랜을 완료 상태로 변경한다.
	 */
	void complete(Long userId, Long planId, Long lessonId);
}
