package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlements;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementModifyService {

	private final EntitlementRepository entitlementRepository;

	@Transactional
	public void synchronize(Long userId, List<ActiveEntitlement> activeEntitlements) {
		entitlementRepository.lockByUserId(userId);

		Entitlements entitlements = new Entitlements(entitlementRepository.findAllByUserId(userId));

		List<Entitlement> granted = entitlements.synchronize(userId, activeEntitlements, Instant.now());

		entitlementRepository.saveAll(granted);
	}
}
