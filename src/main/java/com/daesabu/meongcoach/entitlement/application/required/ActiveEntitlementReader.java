package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import java.util.List;

/**
 * 결제 대행에 기록된 회원의 활성 이용권을 읽는다.
 */
public interface ActiveEntitlementReader {

	/**
	 * 회원이 지금 쓸 수 있는 이용권의 종류와 만료 시각을 반환한다. 만료 시각이 없는 이용권은 만료 시각을 null로 둔다.
	 * 결제 대행에 회원이 없거나 활성 이용권이 없으면 빈 목록을 반환한다.
	 * 결제 대행에서 내역을 받아 오지 못하면 {@code EntitlementProviderUnavailableException}을 던진다.
	 */
	List<ActiveEntitlement> readActiveEntitlements(Long userId);
}
