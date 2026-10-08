package com.daesabu.meongcoach.promotion.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PromotionCodeExpiredException extends DomainException {

	public PromotionCodeExpiredException() {
		super(PromotionErrorCode.PROMOTION_CODE_EXPIRED);
	}
}
