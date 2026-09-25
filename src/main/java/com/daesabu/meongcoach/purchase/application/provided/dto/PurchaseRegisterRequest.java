package com.daesabu.meongcoach.purchase.application.provided.dto;

import com.daesabu.meongcoach.purchase.domain.PurchaseRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * RevenueCat 구매 웹훅 한 건의 등록 입력. 값은 웹훅 원본 그대로다.
 * appUserId는 익명 ID일 수도 있어 회원 ID로 바꾸지 않고 받으며, 회원 식별은 서비스가 한다.
 * entitlementIds는 권한을 주지 않는 상품이면 웹훅에서 null로 오므로 빈 집합으로 바꾼다.
 */
public record PurchaseRegisterRequest(
		@NotBlank String appUserId,
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
