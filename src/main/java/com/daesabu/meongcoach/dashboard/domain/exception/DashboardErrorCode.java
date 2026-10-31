package com.daesabu.meongcoach.dashboard.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum DashboardErrorCode implements ErrorCode {

	DASHBOARD_USER_NOT_FOUND(404, "회원을 찾을 수 없습니다."),
	;

	private final int status;
	private final String message;

	DashboardErrorCode(int status, String message) {
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
