package com.daesabu.meongcoach.promotion.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PromotionCodeExhaustedException extends DomainException {

	public PromotionCodeExhaustedException() {
		super(PromotionErrorCode.PROMOTION_CODE_EXHAUSTED);
	}
}
