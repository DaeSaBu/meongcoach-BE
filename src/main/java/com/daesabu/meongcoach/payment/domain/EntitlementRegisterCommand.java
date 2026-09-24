package com.daesabu.meongcoach.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record EntitlementRegisterCommand(
		Long userId,
		String transactionId,
		ProductId productId,
		Store store,
		BigDecimal price,
		String currency,
		Instant purchasedAt
) {
}
