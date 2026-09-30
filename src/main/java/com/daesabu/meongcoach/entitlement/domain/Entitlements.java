package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	public List<Entitlement> synchronize(Long userId, Set<EntitlementType> activeTypes) {
		updateExistingEntitlementState(activeTypes);

		Set<EntitlementType> existingTypes = getExistingEntitlementTypes();

		return grantNewTypes(userId, activeTypes, existingTypes);
	}

	private static List<Entitlement> grantNewTypes(Long userId, Set<EntitlementType> activeTypes,
	                                               Set<EntitlementType> existingTypes) {
		return activeTypes.stream()
				.filter(type -> !existingTypes.contains(type))
				.map(type -> Entitlement.grant(userId, type))
				.toList();
	}

	private Set<EntitlementType> getExistingEntitlementTypes() {
		return entitlements.stream()
				.map(Entitlement::getType)
				.collect(Collectors.toUnmodifiableSet());
	}

	private void updateExistingEntitlementState(Set<EntitlementType> activeTypes) {
		entitlements.forEach(entitlement -> reflect(entitlement, activeTypes));
	}

	private void reflect(Entitlement entitlement, Set<EntitlementType> activeTypes) {
		boolean shouldBeActive = activeTypes.contains(entitlement.getType());
		if (shouldBeActive && !entitlement.isActive()) {
			entitlement.restore();
			return;
		}
		if (!shouldBeActive && entitlement.isActive()) {
			entitlement.revoke();
		}
	}
}
