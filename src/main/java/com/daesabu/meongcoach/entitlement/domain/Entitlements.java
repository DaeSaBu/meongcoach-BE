package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	public List<Entitlement> synchronize(Long userId, List<ActiveEntitlement> activeEntitlements, Instant now) {
		expireMissing(activeEntitlements, now);
		renewOwned(activeEntitlements);

		return grantUnowned(userId, activeEntitlements);
	}

	private void expireMissing(List<ActiveEntitlement> activeEntitlements, Instant now) {
		Set<EntitlementType> activeTypes = activeEntitlements.stream()
				.map(ActiveEntitlement::type)
				.collect(Collectors.toUnmodifiableSet());

		entitlements.stream()
				.filter(entitlement -> !activeTypes.contains(entitlement.getType()))
				.filter(entitlement -> entitlement.isActive(now))
				.forEach(entitlement -> entitlement.expire(now));
	}

	private void renewOwned(List<ActiveEntitlement> activeEntitlements) {
		activeEntitlements.forEach(activeEntitlement -> findByType(activeEntitlement.type())
				.ifPresent(entitlement -> entitlement.changeExpiresAt(activeEntitlement.expiresAt())));
	}

	private List<Entitlement> grantUnowned(Long userId, List<ActiveEntitlement> activeEntitlements) {
		return activeEntitlements.stream()
				.filter(activeEntitlement -> findByType(activeEntitlement.type()).isEmpty())
				.map(activeEntitlement -> Entitlement.grant(userId, activeEntitlement.type(),
						activeEntitlement.expiresAt()))
				.toList();
	}

	private Optional<Entitlement> findByType(EntitlementType type) {
		return entitlements.stream()
				.filter(entitlement -> entitlement.getType() == type)
				.findFirst();
	}
}
