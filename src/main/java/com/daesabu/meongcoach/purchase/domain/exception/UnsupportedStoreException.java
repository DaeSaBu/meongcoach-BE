package com.daesabu.meongcoach.purchase.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UnsupportedStoreException extends DomainException {

	public UnsupportedStoreException(String store) {
		super(PurchaseErrorCode.PURCHASE_UNSUPPORTED_STORE, "지원하지 않는 스토어입니다: " + store);
	}
}
