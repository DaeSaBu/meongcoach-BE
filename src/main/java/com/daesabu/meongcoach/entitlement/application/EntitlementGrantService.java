package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.PromotionalEntitlementGranter;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntitlementGrantService implements EntitlementGranter {

	private final PromotionalEntitlementGranter promotionalEntitlementGranter;
	private final EntitlementSynchronizer entitlementSynchronizer;

	@Override
	public void grant(Long userId, Instant expiresAt) {
		for (EntitlementType type : EntitlementType.values()) {
			promotionalEntitlementGranter.grant(userId, type, expiresAt);
		}
		entitlementSynchronizer.synchronize(userId);
	}
}
