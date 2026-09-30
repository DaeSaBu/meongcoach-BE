package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 한 회원이 가진 이용권 전체. 영속화 단위가 아니며, application이 리포지토리로 조회한 회원의 이용권 행으로 만든다.
 * 이용권 상태의 원천은 RevenueCat이므로 이 묶음은 스스로 판단하지 않고 RevenueCat이 알려 준 활성 종류에 맞춘다.
 */
public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	/**
	 * 회수된 행이 활성 종류에 있으면 살리고, 활성 행이 활성 종류에서 빠졌으면 회수한 뒤, 한 번도 가진 적 없는 종류는 새로 부여한다.
	 * 이미 활성 종류와 같은 상태인 행은 건드리지 않으므로, 같은 활성 종류로 다시 맞춰도 아무것도 바뀌지 않는다.
	 * 기존 행은 이 자리에서 바뀌고, 새로 부여한 이용권만 돌려주므로 호출자가 저장한다.
	 */
	public List<Entitlement> synchronize(Long userId, Set<EntitlementType> activeTypes, Instant now) {
		entitlements.forEach(entitlement -> reflect(entitlement, activeTypes, now));

		Set<EntitlementType> ownedTypes = entitlements.stream()
				.map(Entitlement::getType)
				.collect(Collectors.toUnmodifiableSet());
		return activeTypes.stream()
				.filter(type -> !ownedTypes.contains(type))
				.map(type -> Entitlement.grant(userId, type))
				.toList();
	}

	private void reflect(Entitlement entitlement, Set<EntitlementType> activeTypes, Instant now) {
		boolean shouldBeActive = activeTypes.contains(entitlement.getType());
		if (shouldBeActive && !entitlement.isActive()) {
			entitlement.restore();
			return;
		}
		if (!shouldBeActive && entitlement.isActive()) {
			entitlement.revoke(now);
		}
	}
}
