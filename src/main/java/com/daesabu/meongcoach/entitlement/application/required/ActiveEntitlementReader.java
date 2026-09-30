package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Set;

/**
 * 결제 대행에 기록된 회원의 활성 이용권 종류를 읽는다.
 */
public interface ActiveEntitlementReader {

	/**
	 * 회원이 지금 쓸 수 있는 이용권 종류를 반환한다. 결제 대행에 회원이 없거나 활성 이용권이 없으면 빈 집합을 반환한다.
	 * 결제 대행에서 내역을 받아 오지 못하면 {@code EntitlementProviderUnavailableException}을 던진다.
	 */
	Set<EntitlementType> readActiveEntitlementTypes(Long userId);
}
