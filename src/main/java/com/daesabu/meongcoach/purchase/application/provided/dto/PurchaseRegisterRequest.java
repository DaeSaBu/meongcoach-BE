package com.daesabu.meongcoach.purchase.application.provided.dto;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.domain.PurchaseRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * 스토어 구매 한 건의 등록 입력. 외부 사용자 ID를 회원 ID로, entitlement 식별자를 이용권 종류로 바꾸는 일은 호출하는 어댑터가 끝내고 넘긴다.
 * entitlementTypes는 이용권을 주지 않는 상품이면 null로 올 수 있어 빈 집합으로 바꾼다.
 */
public record PurchaseRegisterRequest(
		@NotNull Long userId,
		@NotBlank String transactionId,
		@NotBlank String productId,
		@NotBlank String store,
		BigDecimal price,
		String currency,
		@NotNull Instant purchasedAt,
		Set<EntitlementType> entitlementTypes
) {

	public PurchaseRegisterRequest {
		entitlementTypes = Set.copyOf(Objects.requireNonNullElse(entitlementTypes, Set.of()));
	}

	public PurchaseRegisterCommand toCommand() {
		return new PurchaseRegisterCommand(transactionId, productId, store, price, currency, purchasedAt);
	}
}
