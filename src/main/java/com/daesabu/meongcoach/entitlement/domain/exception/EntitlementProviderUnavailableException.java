package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 결제 대행(RevenueCat)에서 활성 이용권을 받아 오지 못한 경우. 이용권이 없는 것과 구분해야 회수하지 않고 클라이언트가 재시도할 수 있다.
 */
public class EntitlementProviderUnavailableException extends DomainException {

	public EntitlementProviderUnavailableException() {
		super(EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE);
	}

	public EntitlementProviderUnavailableException(Throwable cause) {
		super(EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE,
				EntitlementErrorCode.ENTITLEMENT_PROVIDER_UNAVAILABLE.message(), cause);
	}
}
