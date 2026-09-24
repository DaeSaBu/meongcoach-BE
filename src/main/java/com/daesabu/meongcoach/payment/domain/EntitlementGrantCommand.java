package com.daesabu.meongcoach.payment.domain;

import java.time.Instant;

public record EntitlementGrantCommand(
		Long userId,
		ProductId productId,
		Store store,
		Instant grantedAt
) {
}
