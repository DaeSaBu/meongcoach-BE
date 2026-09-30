package com.daesabu.meongcoach.entitlement.application.provided;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;

/**
 * 유료 기능을 열어 주기 전에 회원이 이용권을 가졌는지 검증한다.
 * 우리 DB의 사본만 읽고 결제 대행에 다시 묻지 않는다. 부여 누락은 앱의 동기화 재호출이 메운다.
 */
public interface EntitlementValidator {

	/**
	 * 회원에게 회수되지 않은 해당 종류의 이용권이 없으면 {@code EntitlementNotOwnedException}을 던진다.
	 */
	void validate(Long userId, EntitlementType type);
}
