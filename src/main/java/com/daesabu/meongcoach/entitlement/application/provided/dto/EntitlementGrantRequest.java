package com.daesabu.meongcoach.entitlement.application.provided.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * 구매 한 건으로 얻은 권한의 부여 입력. identifiers는 RevenueCat entitlement 식별자 원본이며, 권한을 주지 않는 상품이면 빈 집합이다.
 */
public record EntitlementGrantRequest(
		@NotNull Long userId,
		@NotNull Long purchaseId,
		@NotNull Set<String> identifiers
) {
}
