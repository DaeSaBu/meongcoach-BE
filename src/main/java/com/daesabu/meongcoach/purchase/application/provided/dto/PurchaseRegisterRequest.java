package com.daesabu.meongcoach.purchase.application.provided.dto;

import com.daesabu.meongcoach.purchase.domain.PurchaseRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * 스토어 구매 한 건의 등록 입력. 외부 사용자 ID를 회원 ID로 바꾸는 일은 호출하는 어댑터가 끝내고 넘긴다.
 * entitlementIds는 권한을 주지 않는 상품이면 null로 올 수 있어 빈 집합으로 바꾼다.
 */
public record PurchaseRegisterRequest(
		@NotNull Long userId,
		@NotBlank String transactionId,
		@NotBlank String productId,
		@NotBlank String store,
		BigDecimal price,
		String currency,
		@NotNull Instant purchasedAt,
		Set<String> entitlementIds
) {

	public PurchaseRegisterRequest {
		entitlementIds = Set.copyOf(Objects.requireNonNullElse(entitlementIds, Set.of()));
	}

	public PurchaseRegisterCommand toCommand() {
		return new PurchaseRegisterCommand(transactionId, productId, store, price, currency, purchasedAt);
	}
}
