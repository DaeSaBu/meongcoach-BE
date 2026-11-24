package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SelfReportNotAllowedException extends DomainException {

	public SelfReportNotAllowedException() {
		super(CommunityErrorCode.COMMUNITY_SELF_REPORT_NOT_ALLOWED);
	}
}
