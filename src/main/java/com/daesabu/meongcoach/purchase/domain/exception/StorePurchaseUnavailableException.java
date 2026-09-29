package com.daesabu.meongcoach.purchase.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 결제 대행(RevenueCat)에서 구매 내역을 받아 오지 못한 경우. 구매가 없는 것과 구분해 클라이언트가 재시도하게 한다.
 */
public class StorePurchaseUnavailableException extends DomainException {

	public StorePurchaseUnavailableException() {
		super(PurchaseErrorCode.PURCHASE_STORE_UNAVAILABLE);
	}

	public StorePurchaseUnavailableException(Throwable cause) {
		super(PurchaseErrorCode.PURCHASE_STORE_UNAVAILABLE, PurchaseErrorCode.PURCHASE_STORE_UNAVAILABLE.message(),
				cause);
	}
}
