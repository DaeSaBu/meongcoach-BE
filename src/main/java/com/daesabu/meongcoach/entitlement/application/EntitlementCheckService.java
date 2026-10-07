package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementCheckService implements EntitlementChecker {

	private final EntitlementRepository entitlementRepository;

	@Override
	public boolean hasEntitlement(Long userId, EntitlementType type) {
		Instant now = Instant.now();
		return entitlementRepository.findByUserIdAndType(userId, type)
				.map(entitlement -> entitlement.isActive(now))
				.orElse(false);
	}

	@Override
	public void validateEntitlement(Long userId, EntitlementType type) {
		if (hasEntitlement(userId, type)) {
			return;
		}
		throw new EntitlementRequiredException();
	}
}
