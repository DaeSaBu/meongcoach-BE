package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Collection;
import java.util.List;

/**
 * 구매 한 건으로 얻은 이용권 묶음. 통합 상품처럼 한 구매가 여러 이용권을 주는 경우 그 수만큼 {@link Entitlement}를 만든다.
 * 영속화 단위가 아니며 저장은 호출자가 한다.
 */
public class Entitlements {

	private final List<Entitlement> entitlements;

	private Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	/**
	 * 이용권 종류마다 이용권을 만든다. 이용권을 주지 않는 상품이면 빈 묶음이다.
	 * 이용권이 구매를 ID로 가리키므로 저장되어 ID가 있는 구매의 ID를 받는다.
	 */
	public static Entitlements grant(Long userId, Long purchaseId, Collection<EntitlementType> types) {
		List<Entitlement> granted = types.stream()
				.map(type -> Entitlement.grant(userId, purchaseId, type))
				.toList();

		return new Entitlements(granted);
	}

	public List<Entitlement> toList() {
		return entitlements;
	}
}
