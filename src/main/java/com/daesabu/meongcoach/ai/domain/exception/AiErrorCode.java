package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum AiErrorCode implements ErrorCode {

	AI_REPORT_NOT_FOUND(404, "AI 리포트를 찾을 수 없습니다."),
	AI_REPORT_TRIAL_EXCEEDED(403, "AI 리포트 무료 체험 횟수를 모두 사용했습니다."),
	// 아래 둘은 비동기 컨슈머 안에서만 발생해 HTTP 응답으로 나가지 않지만, 외부 연동 실패라 502로 둔다
	AI_VIDEO_ANALYSIS_FAILED(502, "영상 분석에 실패했습니다."),
	AI_REPORT_TITLE_GENERATION_FAILED(502, "리포트 제목 생성에 실패했습니다.");

	private final int status;
	private final String message;

	AiErrorCode(int status, String message) {
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
