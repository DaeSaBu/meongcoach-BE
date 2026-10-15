package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;

/**
 * 결제 대행에 프로모션 이용권 지급을 요청하는 포트.
 */
public interface PromotionalEntitlementGranter {

	/**
	 * 회원에게 한 종류의 이용권을 주어진 만료 시각까지 지급한다.
	 * 결제 대행이 요청을 처리하지 못하면 {@code EntitlementProviderUnavailableException}을 던진다.
	 */
	void grant(Long userId, EntitlementType type, Instant expiresAt);
}
