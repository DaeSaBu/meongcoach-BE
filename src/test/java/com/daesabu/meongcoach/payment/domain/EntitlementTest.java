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
	private static final Instant REVOKED_AT = Instant.parse("2026-09-25T03:00:00Z");
	private static final EntitlementRegisterCommand COMMAND = new EntitlementRegisterCommand(
			TRANSACTION_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, PRICE, CURRENCY, PURCHASED_AT
	);

	@Test
	void 부여하면_회원과_거래와_상품과_스토어와_결제_가격과_구매_시각이_담기고_활성_상태다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);

		assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
		assertThat(entitlement.getTransactionId()).isEqualTo(TRANSACTION_ID);
		assertThat(entitlement.getProductId()).isEqualTo(ProductId.PUPPY_LIFETIME);
		assertThat(entitlement.getStore()).isEqualTo(Store.APP_STORE);
		assertThat(entitlement.getPrice()).isEqualTo(PRICE);
		assertThat(entitlement.getCurrency()).isEqualTo(CURRENCY);
		assertThat(entitlement.getPurchasedAt()).isEqualTo(PURCHASED_AT);
		assertThat(entitlement.getRevokedAt()).isNull();
		assertThat(entitlement.isActive()).isTrue();
	}

	@Test
	void 상품이_주는_시기에만_권한을_준다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);

		assertThat(entitlement.grants(LifeStage.PUPPY)).isTrue();
		assertThat(entitlement.grants(LifeStage.ADULT)).isFalse();
	}

	@Test
	void 회수된_구매는_어느_시기에도_권한을_주지_않는다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);
		entitlement.revoke(REVOKED_AT);

		boolean grants = entitlement.grants(LifeStage.PUPPY);

		assertThat(grants).isFalse();
	}

	@Test
	void 회수하면_revokedAt이_기록되고_비활성_상태가_된다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);

		entitlement.revoke(REVOKED_AT);

		assertThat(entitlement.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(entitlement.isActive()).isFalse();
	}

	@Test
	void 이미_회수된_이용권을_다시_회수해도_처음_시각이_유지된다() {
		Entitlement entitlement = Entitlement.register(USER_ID, COMMAND);
		entitlement.revoke(REVOKED_AT);

		entitlement.revoke(REVOKED_AT.plusSeconds(60));

		assertThat(entitlement.getRevokedAt()).isEqualTo(REVOKED_AT);
	}
}
