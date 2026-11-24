package com.daesabu.meongcoach.report.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum ReportErrorCode implements ErrorCode {

	REPORT_REASON_INVALID(400, "지원하지 않는 신고 사유입니다."),
	REPORT_ALREADY_REGISTERED(409, "이미 신고한 대상입니다.");

	private final int status;
	private final String message;

	ReportErrorCode(int status, String message) {
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
