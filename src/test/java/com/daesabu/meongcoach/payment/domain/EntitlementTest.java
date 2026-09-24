package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntitlementTest {

	private static final Long USER_ID = 1L;
	private static final String TRANSACTION_ID = "170000869511114";
	private static final BigDecimal PRICE = new BigDecimal("9900");
	private static final String CURRENCY = "KRW";
	private static final Instant PURCHASED_AT = Instant.parse("2026-09-24T03:00:00Z");
	private static final EntitlementRegisterCommand COMMAND = new EntitlementRegisterCommand(
			TRANSACTION_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, PRICE, CURRENCY, PURCHASED_AT
	);

	@Test
	void 등록하면_회원과_거래와_상품과_스토어와_결제_가격과_구매_시각이_담기고_회수되지_않은_상태다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);

		assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
		assertThat(entitlement.getTransactionId()).isEqualTo(TRANSACTION_ID);
		assertThat(entitlement.getProductId()).isEqualTo(ProductId.PUPPY_LIFETIME);
		assertThat(entitlement.getStore()).isEqualTo(Store.APP_STORE);
		assertThat(entitlement.getPrice()).isEqualTo(PRICE);
		assertThat(entitlement.getCurrency()).isEqualTo(CURRENCY);
		assertThat(entitlement.getPurchasedAt()).isEqualTo(PURCHASED_AT);
		assertThat(entitlement.getRevokedAt()).isNull();
	}
}
