package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UnsupportedEntitlementTypeException extends DomainException {

	public UnsupportedEntitlementTypeException(String type) {
		super(EntitlementErrorCode.ENTITLEMENT_UNSUPPORTED_TYPE, "지원하지 않는 이용권 종류입니다: " + type);
	}
}
