package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class EntitlementFixture {

	private EntitlementFixture() {
	}

	public static Entitlement create(Long userId, EntitlementType type, Instant expiresAt) {
		Entitlement entitlement = new Entitlement();
		ReflectionTestUtils.setField(entitlement, "userId", userId);
		ReflectionTestUtils.setField(entitlement, "type", type);
		ReflectionTestUtils.setField(entitlement, "expiresAt", expiresAt);
		return entitlement;
	}
}
