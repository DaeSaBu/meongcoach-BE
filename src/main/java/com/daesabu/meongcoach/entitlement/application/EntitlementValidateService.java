package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementValidator;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementNotOwnedException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementValidateService implements EntitlementValidator {

	private final EntitlementRepository entitlementRepository;

	@Override
	public void validate(Long userId, EntitlementType type) {
		if (!entitlementRepository.existsByUserIdAndTypeAndRevokedAtIsNull(userId, type)) {
			throw new EntitlementNotOwnedException(type);
		}
	}
}
