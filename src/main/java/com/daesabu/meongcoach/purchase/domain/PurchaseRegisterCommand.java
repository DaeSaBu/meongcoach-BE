package com.daesabu.meongcoach.purchase.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 스토어 구매 한 건의 등록 입력. 값 검증은 PurchaseRegisterRequest가 서비스 경계에서 끝내고 넘긴다.
 * price·currency는 RevenueCat이 USD로 환산한 매출(revenue_in_usd)이다.
 */
public record PurchaseRegisterCommand(
		String transactionId,
		Store store,
		BigDecimal price,
		String currency,
		Instant purchasedAt
) {
}
