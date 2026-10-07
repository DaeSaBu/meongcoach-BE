package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	public List<Entitlement> synchronize(Long userId, List<ActiveEntitlement> activeEntitlements, Instant now) {
		Map<EntitlementType, ActiveEntitlement> activeEntitlementsByType = activeEntitlements.stream()
				.collect(Collectors.toUnmodifiableMap(ActiveEntitlement::type, Function.identity()));

		updateExistingEntitlementState(activeEntitlementsByType, now);

		Set<EntitlementType> existingTypes = getExistingEntitlementTypes();

		return grantNewTypes(userId, activeEntitlements, existingTypes);
	}

	private static List<Entitlement> grantNewTypes(Long userId, List<ActiveEntitlement> activeEntitlements,
	                                               Set<EntitlementType> existingTypes) {
		return activeEntitlements.stream()
				.filter(activeEntitlement -> !existingTypes.contains(activeEntitlement.type()))
				.map(activeEntitlement -> Entitlement.grant(userId, activeEntitlement.type(),
						activeEntitlement.expiresAt()))
				.toList();
	}

	private Set<EntitlementType> getExistingEntitlementTypes() {
		return entitlements.stream()
				.map(Entitlement::getType)
				.collect(Collectors.toUnmodifiableSet());
	}

	private void updateExistingEntitlementState(Map<EntitlementType, ActiveEntitlement> activeEntitlementsByType,
	                                            Instant now) {
		entitlements.forEach(entitlement -> reflect(entitlement, activeEntitlementsByType, now));
	}

	private void reflect(Entitlement entitlement, Map<EntitlementType, ActiveEntitlement> activeEntitlementsByType,
	                     Instant now) {
		ActiveEntitlement activeEntitlement = activeEntitlementsByType.get(entitlement.getType());
		if (activeEntitlement != null) {
			entitlement.changeExpiresAt(activeEntitlement.expiresAt());
			return;
		}
		if (entitlement.isActive(now)) {
			entitlement.expire(now);
		}
	}
}
