package com.daesabu.meongcoach.purchase.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PurchaseTest {

	private static final Long USER_ID = 1L;
	private static final String TRANSACTION_ID = "170000869511114";
	private static final String PRODUCT_ID = "meongcoach_puppy_lifetime";
	private static final String STORE = "APP_STORE";
	private static final BigDecimal PRICE = new BigDecimal("9900");
	private static final String CURRENCY = "KRW";
	private static final Instant PURCHASED_AT = Instant.parse("2026-09-24T03:00:00Z");
	private static final PurchaseRegisterCommand COMMAND = new PurchaseRegisterCommand(
			TRANSACTION_ID, PRODUCT_ID, STORE, PRICE, CURRENCY, PURCHASED_AT
	);

	@Test
	void 등록하면_회원과_거래와_상품과_스토어와_결제_가격과_구매_시각이_담기고_환불되지_않은_상태다() {
		Purchase purchase = Purchase.register(USER_ID, COMMAND);

		assertThat(purchase.getUserId()).isEqualTo(USER_ID);
		assertThat(purchase.getTransactionId()).isEqualTo(TRANSACTION_ID);
		assertThat(purchase.getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(purchase.getStore()).isEqualTo(STORE);
		assertThat(purchase.getPrice()).isEqualTo(PRICE);
		assertThat(purchase.getCurrency()).isEqualTo(CURRENCY);
		assertThat(purchase.getPurchasedAt()).isEqualTo(PURCHASED_AT);
		assertThat(purchase.getRefundedAt()).isNull();
	}
}
