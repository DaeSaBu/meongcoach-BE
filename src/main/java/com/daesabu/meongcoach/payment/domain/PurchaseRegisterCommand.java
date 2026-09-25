package com.daesabu.meongcoach.payment.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 스토어 구매 한 건의 등록 입력. productId·store는 RevenueCat 웹훅 값 원본이다.
 * price·currency는 스토어가 알려 주지 않으면 null이라 제약을 두지 않는다.
 */
public record PurchaseRegisterCommand(
		@NotBlank String transactionId,
		@NotBlank String productId,
		@NotBlank String store,
		BigDecimal price,
		String currency,
		@NotNull Instant purchasedAt
) {
}
