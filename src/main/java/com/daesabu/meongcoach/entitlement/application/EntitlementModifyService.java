package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlements;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementModifyService implements EntitlementSynchronizer {

	private final EntitlementRepository entitlementRepository;

	@Override
	@Transactional
	public void synchronize(Long userId, Set<EntitlementType> activeTypes) {
		Entitlements entitlements = new Entitlements(entitlementRepository.findAllByUserId(userId));

		List<Entitlement> granted = entitlements.synchronize(userId, activeTypes);

		entitlementRepository.saveAll(granted);
	}
}
