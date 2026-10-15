package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum PlanErrorCode implements ErrorCode {

	PLAN_INVALID_MODE(400, "플랜 생성 방식이 올바르지 않습니다."),
	PLAN_INVALID_OUTCOME(400, "플랜 마무리 결과가 올바르지 않습니다."),
	PLAN_EXPECTATION_REQUIRED(400, "기대사항은 5자 이상 200자 이하로 입력해 주세요."),
	PLAN_TOPICS_REQUIRED(400, "직접 고르기는 토픽을 1개 이상 3개 이하로 선택해 주세요."),
	PLAN_TOPIC_NOT_FOUND(404, "선택한 토픽을 찾을 수 없습니다."),
	PLAN_NOT_FOUND(404, "플랜을 찾을 수 없습니다."),
	PLAN_ALREADY_ACTIVE(409, "이미 진행 중인 플랜이 있습니다."),
	PLAN_NOT_ACTIVE(409, "진행 중인 플랜이 아닙니다."),
	PLAN_CATALOG_EMPTY(409, "플랜에 담을 수 있는 열린 커리큘럼이 없습니다."),
	PLAN_GENERATION_FAILED(502, "AI 플랜 생성에 실패했습니다."),
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
