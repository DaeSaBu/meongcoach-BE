package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EntitlementsTest {

	private static final Long USER_ID = 1L;
	private static final Instant PURCHASED_AT = Instant.parse("2026-09-24T03:00:00Z");

	@Test
	void 처음_보는_거래면_구매_기록을_만든다() {
		Entitlements entitlements = new Entitlements(List.of());

		Optional<Entitlement> granted = entitlements.grant(USER_ID, command("tx-1"));

		assertThat(granted).get()
				.extracting(Entitlement::getTransactionId)
				.isEqualTo("tx-1");
	}

	@Test
	void 같은_거래로_다시_등록하면_새_구매를_만들지_않는다() {
		List<Entitlement> owned = new Entitlements(List.of()).grant(USER_ID, command("tx-1")).stream().toList();
		Entitlements entitlements = new Entitlements(owned);

		Optional<Entitlement> granted = entitlements.grant(USER_ID, command("tx-1"));

		assertThat(granted).isEmpty();
	}

	private EntitlementRegisterCommand command(String transactionId) {
		return new EntitlementRegisterCommand(
				transactionId, ProductId.ALL_LIFETIME, Store.APP_STORE, new BigDecimal("9900"), "KRW", PURCHASED_AT
		);
	}
}
