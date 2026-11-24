package com.daesabu.meongcoach.report.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidReportReasonException extends DomainException {

	public InvalidReportReasonException(String code) {
		super(ReportErrorCode.REPORT_REASON_INVALID, "지원하지 않는 신고 사유입니다: " + code);
	}
}
