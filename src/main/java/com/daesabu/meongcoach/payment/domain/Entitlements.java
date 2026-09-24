package com.daesabu.meongcoach.payment.domain;

import java.time.Instant;
import java.util.List;

/**
 * 한 사용자의 이용권 전체. 구매·권한 확인·환불 회수처럼 이용권 하나로는 판단할 수 없는 사용자 단위 규칙을 담는다.
 * 영속화 단위가 아니라 application이 조회한 목록으로 만드는 일급 컬렉션이며, 리포지토리를 알지 못한다.
 * 행은 상품 기준으로 항상 모두 만들고, 권한은 그 시기의 활성 이용권이 하나라도 있으면 있다고 본다.
 */
public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	/**
	 * 상품이 주는 시기마다 새 이용권을 만들어 반환한다. 저장은 호출자가 한다.
	 * 이미 가진 시기도 새로 만든다 — 건너뛰면 먼저 산 개별 이용권을 환불할 때 나중에 산 통합 이용권의 권한까지 사라진다.
	 */
	public List<Entitlement> grant(EntitlementGrantCommand command) {
		return command.productId().getLifeStages().stream()
				.map(lifeStage -> Entitlement.grant(lifeStage, command))
				.toList();
	}

	public boolean hasAccess(LifeStage lifeStage) {
		return entitlements.stream()
				.filter(Entitlement::isActive)
				.anyMatch(entitlement -> entitlement.getLifeStage() == lifeStage);
	}

	/**
	 * 그 상품으로 받은 활성 이용권만 회수한다. 해당 이용권이 없으면 아무것도 하지 않아 같은 웹훅이 다시 와도 안전하다.
	 */
	public void revoke(ProductId productId, Instant revokedAt) {
		entitlements.stream()
				.filter(Entitlement::isActive)
				.filter(entitlement -> entitlement.getProductId() == productId)
				.forEach(entitlement -> entitlement.revoke(revokedAt));
	}
}
