package com.daesabu.meongcoach.report.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ReportAlreadyRegisteredException extends DomainException {

	public ReportAlreadyRegisteredException() {
		super(ReportErrorCode.REPORT_ALREADY_REGISTERED);
	}
}
