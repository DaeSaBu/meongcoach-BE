package com.daesabu.meongcoach.alert.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AlertDeliveryFailedException extends DomainException {

	public AlertDeliveryFailedException(String detail) {
		super(AlertErrorCode.ALERT_DELIVERY_FAILED, detail);
	}
}
