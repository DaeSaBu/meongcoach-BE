package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.entitlement.application.provided.dto.EntitlementGrantRequest;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EntitlementGrantService implements EntitlementGranter {

	private final EntitlementRepository entitlementRepository;

	@Override
	@Transactional
	public void grant(EntitlementGrantRequest entitlementGrantRequest) {
		Entitlements entitlements = Entitlements.grant(
				entitlementGrantRequest.userId(),
				entitlementGrantRequest.purchaseId(),
				entitlementGrantRequest.identifiers()
		);
		entitlementRepository.saveAll(entitlements.toList());
	}
}
