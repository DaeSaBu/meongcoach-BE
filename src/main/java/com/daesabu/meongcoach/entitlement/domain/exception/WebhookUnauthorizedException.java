package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * RevenueCat 웹훅의 인증값이 없거나 설정과 다른 경우. 웹훅 경로는 JWT 없이 열려 있어 이 값이 유일한 발신자 확인이다.
 */
public class WebhookUnauthorizedException extends DomainException {

	public WebhookUnauthorizedException() {
		super(EntitlementErrorCode.ENTITLEMENT_WEBHOOK_UNAUTHORIZED);
	}
}
