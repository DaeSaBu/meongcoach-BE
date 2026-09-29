package com.daesabu.meongcoach.entitlement.application.provided.dto;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * 구매 한 건으로 얻은 이용권의 부여 입력. 이용권을 주지 않는 상품이면 types는 빈 집합이다.
 */
public record EntitlementGrantRequest(
		@NotNull Long userId,
		@NotNull Long purchaseId,
		@NotNull Set<EntitlementType> types
) {
}
