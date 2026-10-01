package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class EntitlementProviderUnavailableException extends DomainException {

	public EntitlementProviderUnavailableException() {
		super(EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE);
	}

	public EntitlementProviderUnavailableException(Throwable cause) {
		super(EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE,
				EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE.message(), cause);
	}
}
