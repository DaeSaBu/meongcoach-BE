package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanCatalogEmptyException extends DomainException {

	public PlanCatalogEmptyException() {
		super(PlanErrorCode.PLAN_CATALOG_EMPTY);
	}
}
