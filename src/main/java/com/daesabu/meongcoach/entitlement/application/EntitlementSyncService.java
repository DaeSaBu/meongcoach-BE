package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementSyncService implements EntitlementSynchronizer {

	private final ActiveEntitlementReader activeEntitlementReader;
	private final EntitlementModifyService entitlementModifyService;

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void synchronize(Long userId) {
		Set<EntitlementType> activeEntitlementTypes = activeEntitlementReader.readActiveEntitlementTypes(userId);

		entitlementModifyService.synchronize(userId, activeEntitlementTypes);
	}
}
