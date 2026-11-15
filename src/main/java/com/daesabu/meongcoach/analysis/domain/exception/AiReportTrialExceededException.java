package com.daesabu.meongcoach.analysis.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiReportTrialExceededException extends DomainException {

	public AiReportTrialExceededException() {
		super(AiErrorCode.AI_REPORT_TRIAL_EXCEEDED);
	}
}
