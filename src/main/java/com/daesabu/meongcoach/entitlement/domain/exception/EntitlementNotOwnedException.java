package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 회원에게 회수되지 않은 해당 종류의 이용권이 없는 경우. 앱은 이 코드를 받으면 페이월을 띄운다.
 */
public class EntitlementNotOwnedException extends DomainException {

	public EntitlementNotOwnedException(EntitlementType type) {
		super(EntitlementErrorCode.ENTITLEMENT_NOT_OWNED, "이용권이 필요한 기능입니다: " + type);
	}
}
