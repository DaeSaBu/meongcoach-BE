package com.daesabu.meongcoach.payment.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 스토어 구매 한 건의 등록 입력. price·currency는 스토어가 알려 주지 않으면 null이라 제약을 두지 않는다.
 */
public record EntitlementRegisterCommand(
		@NotBlank String transactionId,
		@NotNull ProductId productId,
		@NotNull Store store,
		BigDecimal price,
		String currency,
		@NotNull Instant purchasedAt
) {
}
