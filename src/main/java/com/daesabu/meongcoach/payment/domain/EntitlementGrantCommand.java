package com.daesabu.meongcoach.payment.domain;

import java.time.Instant;

public record EntitlementGrantCommand(
		Long userId,
		String transactionId,
		ProductId productId,
		Store store,
		Instant purchasedAt
) {
}
