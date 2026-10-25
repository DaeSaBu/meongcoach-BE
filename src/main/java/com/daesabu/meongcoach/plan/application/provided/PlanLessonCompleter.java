package com.daesabu.meongcoach.plan.application.provided;

public interface PlanLessonCompleter {

	/**
	 * 사용자가 소유한 활성 플랜에서 현재 진행할 레슨을 완료한다.
	 * 일반 학습 기록과 플랜의 완료 기록을 반영하고, 모든 레슨을 완료하면 플랜을 완료 상태로 변경한다.
	 * 플랜이 없거나 사용자의 소유가 아니면 {@code PlanNotFoundException}을 던진다.
	 * 교체된 플랜이면 {@code PlanNotActiveException}을 던진다.
	 * 활성 또는 완료된 플랜에 이미 기록된 레슨은 일반 학습 횟수와 플랜 기록을 변경하지 않고 반환한다.
	 * 완료된 플랜에 기록되지 않은 레슨을 요청하면 {@code PlanNotActiveException}을 던진다.
	 */
	void complete(Long userId, Long planId, Long lessonId);
}
