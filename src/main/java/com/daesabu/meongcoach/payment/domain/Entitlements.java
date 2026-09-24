package com.daesabu.meongcoach.payment.domain;

import java.time.Instant;
import java.util.List;

/**
 * 한 사용자의 이용권 전체. 구매·권한 확인·환불 회수처럼 이용권 하나로는 판단할 수 없는 사용자 단위 규칙을 담는다.
 * 영속화 단위가 아니라 application이 조회한 목록으로 만드는 일급 컬렉션이며, 리포지토리를 알지 못한다.
 * 부여·회수는 스토어 거래 단위로 하고, 권한은 그 시기의 활성 이용권이 하나라도 있으면 있다고 본다.
 */
public class Entitlements {

	private final List<Entitlement> entitlements;

	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	/**
	 * 상품이 주는 시기마다 새 이용권을 만들어 반환한다. 저장은 호출자가 한다.
	 * 이미 처리한 거래면 빈 목록을 반환해 같은 구매 웹훅이 다시 와도 행이 중복되지 않는다.
	 * 다른 거래로 이미 가진 시기도 새로 만든다 — 건너뛰면 먼저 산 개별 이용권을 환불할 때 나중에 산 통합 이용권의 권한까지 사라진다.
	 */
	public List<Entitlement> grant(EntitlementGrantCommand command) {
		if (hasTransaction(command.transactionId())) {
			return List.of();
		}
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
	 * 그 거래로 받은 활성 이용권만 회수한다. 해당 이용권이 없으면 아무것도 하지 않는다.
	 * 상품이 아닌 거래로 고르므로, 환불 후 재구매한 뒤 예전 환불 웹훅이 다시 와도 새 구매는 회수되지 않는다.
	 */
	public void revoke(String transactionId, Instant revokedAt) {
		entitlements.stream()
				.filter(Entitlement::isActive)
				.filter(entitlement -> entitlement.getTransactionId().equals(transactionId))
				.forEach(entitlement -> entitlement.revoke(revokedAt));
	}

	private boolean hasTransaction(String transactionId) {
		return entitlements.stream()
				.anyMatch(entitlement -> entitlement.getTransactionId().equals(transactionId));
	}
}
