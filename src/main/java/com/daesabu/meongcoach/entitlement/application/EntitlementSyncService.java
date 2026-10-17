package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntitlementSyncService implements EntitlementSynchronizer {

	private final ActiveEntitlementReader activeEntitlementReader;
	private final EntitlementModifyService entitlementModifyService;

	@Override
	public void synchronize(Long userId) {
		List<ActiveEntitlement> activeEntitlements = activeEntitlementReader.readActiveEntitlements(userId);

		entitlementModifyService.synchronize(userId, activeEntitlements);
	}
}
