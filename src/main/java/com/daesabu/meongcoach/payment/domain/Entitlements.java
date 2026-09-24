package com.daesabu.meongcoach.payment.domain;

import java.util.List;
import java.util.Optional;

/**
 * 한 사용자의 이용권 전체. 같은 거래의 중복 등록처럼 이용권 하나로는 판단할 수 없는 사용자 단위 규칙을 담는다.
 * 영속화 단위가 아니라 application이 조회한 목록으로 만드는 일급 컬렉션이며, 리포지토리를 알지 못한다.
 */
public class Entitlements {

	private final List<Entitlement> entitlements;

	/**
	 * 한 사용자의 이용권을 회수된 것까지 모두 받는다.
	 * 활성 이용권만 넘기면 환불된 거래의 구매 웹훅이 다시 올 때 처음 보는 거래로 여겨 다시 등록한다.
	 */
	public Entitlements(List<Entitlement> entitlements) {
		this.entitlements = List.copyOf(entitlements);
	}

	/**
	 * 처음 보는 거래면 구매 기록을 만들어 반환한다. 저장은 호출자가 한다.
	 * 이미 처리한 거래면 비어 있는 값을 반환해 같은 구매 웹훅이 다시 와도 행이 중복되지 않는다.
	 */
	public Optional<Entitlement> grant(Long userId, EntitlementRegisterCommand command) {
		if (hasTransaction(command.transactionId())) {
			return Optional.empty();
		}
		return Optional.of(Entitlement.register(userId, command));
	}

	private boolean hasTransaction(String transactionId) {
		return entitlements.stream()
				.anyMatch(entitlement -> entitlement.getTransactionId().equals(transactionId));
	}
}
