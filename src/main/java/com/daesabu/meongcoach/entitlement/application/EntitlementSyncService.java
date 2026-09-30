package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementSyncService implements EntitlementSynchronizer {

	private final UserFinder userFinder;
	private final ActiveEntitlementReader activeEntitlementReader;
	private final EntitlementModifyService entitlementModifyService;

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void synchronize(Long userId) {
		Set<EntitlementType> activeTypes = activeEntitlementReader.readActiveTypes(userId);
		if (!userFinder.isActiveUser(userId)) {
			log.warn("탈퇴한 회원이라 이용권을 동기화하지 않음: userId={}", userId);
			return;
		}

		entitlementModifyService.synchronize(userId, activeTypes);
	}
}
