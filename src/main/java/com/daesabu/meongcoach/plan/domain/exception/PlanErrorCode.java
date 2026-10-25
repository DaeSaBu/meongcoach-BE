package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum PlanErrorCode implements ErrorCode {

	PLAN_INVALID_DAILY_MINUTES(400, "하루 훈련 시간은 5, 10, 20 중 하나여야 합니다. 20은 20분 이상을 의미합니다."),
	PLAN_INVALID_DAYS_PER_WEEK(400, "주당 훈련 일수는 3, 5, 7 중 하나여야 합니다."),
	PLAN_EMPTY_CURRICULUMS(400, "플랜에는 커리큘럼이 하나 이상 있어야 합니다."),
	PLAN_DUPLICATE_CURRICULUM(400, "플랜에 같은 커리큘럼을 중복해서 포함할 수 없습니다."),
	PLAN_INVALID_CURRICULUM(400, "플랜의 커리큘럼 ID는 null일 수 없습니다."),
	PLAN_CREATION_CONFLICT(409, "플랜이 변경되었습니다. 현재 플랜을 다시 확인해주세요."),
	;

	private final int status;
	private final String message;

	PlanErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
