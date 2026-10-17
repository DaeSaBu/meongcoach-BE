package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class EntitlementRequiredException extends DomainException {

	public EntitlementRequiredException() {
		super(EntitlementErrorCode.ENTITLEMENT_REQUIRED);
	}
}
