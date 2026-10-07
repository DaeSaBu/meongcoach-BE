package com.daesabu.meongcoach.entitlement.application.provided;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;

/**
 * 회원의 이용권 보유 확인 공개 API.
 */
public interface EntitlementChecker {

	/**
	 * 회원이 해당 종류의 이용권을 지금 쓸 수 있으면 true를 반환한다.
	 * 이용권이 없거나 만료 시각이 지났으면 false를 반환한다.
	 */
	boolean hasEntitlement(Long userId, EntitlementType type);

	/**
	 * 회원이 해당 종류의 이용권을 지금 쓸 수 없으면 {@code EntitlementRequiredException}을 던진다.
	 */
	void validateEntitlement(Long userId, EntitlementType type);
}
