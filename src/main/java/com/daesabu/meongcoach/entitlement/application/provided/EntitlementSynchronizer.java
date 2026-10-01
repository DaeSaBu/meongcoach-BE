package com.daesabu.meongcoach.entitlement.application.provided;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Set;

/**
 * 이용권 동기화 공개 API.
 */
public interface EntitlementSynchronizer {

	/**
	 * 회원의 이용권을 결제 대행이 알려 준 활성 종류에 맞춘다.
	 * 활성 종류에서 빠진 이용권은 회수하고, 다시 들어온 이용권은 복구하며, 없던 종류만 새로 부여한다.
	 */
	void synchronize(Long userId, Set<EntitlementType> activeTypes);
}
