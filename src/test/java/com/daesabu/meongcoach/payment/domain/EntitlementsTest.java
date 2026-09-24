package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EntitlementsTest {

	private static final Long USER_ID = 1L;
	private static final Instant PURCHASED_AT = Instant.parse("2026-09-24T03:00:00Z");
	private static final Instant REVOKED_AT = Instant.parse("2026-09-25T03:00:00Z");

	@Test
	void 처음_보는_거래면_구매_기록을_만든다() {
		Entitlements entitlements = new Entitlements(List.of());

		Optional<Entitlement> granted = entitlements.grant(command("tx-1", ProductId.ALL_LIFETIME));

		assertThat(granted).get()
				.extracting(Entitlement::getTransactionId)
				.isEqualTo("tx-1");
	}

	@Test
	void 같은_거래로_다시_부여하면_새_구매를_만들지_않는다() {
		Entitlements entitlements = new Entitlements(owned("tx-1", ProductId.ALL_LIFETIME));

		Optional<Entitlement> granted = entitlements.grant(command("tx-1", ProductId.ALL_LIFETIME));

		assertThat(granted).isEmpty();
	}

	@Test
	void 퍼피_이용권을_사면_PUPPY_시기에만_권한이_생긴다() {
		Entitlements entitlements = new Entitlements(owned("tx-1", ProductId.PUPPY_LIFETIME));

		assertThat(entitlements.hasAccess(LifeStage.PUPPY)).isTrue();
		assertThat(entitlements.hasAccess(LifeStage.ADULT)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(LifeStage.class)
	void 통합_이용권을_사면_모든_시기에_권한이_생긴다(LifeStage lifeStage) {
		Entitlements entitlements = new Entitlements(owned("tx-1", ProductId.ALL_LIFETIME));

		boolean hasAccess = entitlements.hasAccess(lifeStage);

		assertThat(hasAccess).isTrue();
	}

	@Test
	void 회수된_이용권만_있는_시기는_권한이_없다() {
		Entitlements entitlements = new Entitlements(owned("tx-1", ProductId.PUPPY_LIFETIME));
		entitlements.revoke("tx-1", REVOKED_AT);

		boolean hasAccess = entitlements.hasAccess(LifeStage.PUPPY);

		assertThat(hasAccess).isFalse();
	}

	@Test
	void 거래를_회수하면_그_거래로_받은_이용권만_회수된다() {
		List<Entitlement> puppy = owned("tx-1", ProductId.PUPPY_LIFETIME);
		List<Entitlement> all = owned("tx-2", ProductId.ALL_LIFETIME);
		Entitlements entitlements = new Entitlements(concat(puppy, all));

		entitlements.revoke("tx-1", REVOKED_AT);

		assertThat(puppy).allMatch(entitlement -> REVOKED_AT.equals(entitlement.getRevokedAt()));
		assertThat(all).allMatch(Entitlement::isActive);
	}

	@Test
	void 개별_이용권을_환불해도_통합_이용권이_있으면_그_시기_권한이_유지된다() {
		Entitlements entitlements = new Entitlements(
				concat(owned("tx-1", ProductId.PUPPY_LIFETIME), owned("tx-2", ProductId.ALL_LIFETIME))
		);

		entitlements.revoke("tx-1", REVOKED_AT);

		assertThat(entitlements.hasAccess(LifeStage.PUPPY)).isTrue();
	}

	@Test
	void 환불_후_재구매했다면_예전_거래를_다시_회수해도_새_구매는_유지된다() {
		List<Entitlement> refunded = owned("tx-1", ProductId.PUPPY_LIFETIME);
		new Entitlements(refunded).revoke("tx-1", REVOKED_AT);
		List<Entitlement> repurchased = owned("tx-2", ProductId.PUPPY_LIFETIME);
		Entitlements entitlements = new Entitlements(concat(refunded, repurchased));

		entitlements.revoke("tx-1", REVOKED_AT.plusSeconds(60));

		assertThat(entitlements.hasAccess(LifeStage.PUPPY)).isTrue();
	}

	@Test
	void 가진_적_없는_거래를_회수해도_아무것도_바뀌지_않는다() {
		List<Entitlement> puppy = owned("tx-1", ProductId.PUPPY_LIFETIME);
		Entitlements entitlements = new Entitlements(puppy);

		entitlements.revoke("tx-unknown", REVOKED_AT);

		assertThat(puppy).allMatch(Entitlement::isActive);
	}

	private EntitlementGrantCommand command(String transactionId, ProductId productId) {
		return new EntitlementGrantCommand(
				USER_ID, transactionId, productId, Store.APP_STORE, new BigDecimal("9900"), "KRW", PURCHASED_AT
		);
	}

	private List<Entitlement> owned(String transactionId, ProductId productId) {
		return new Entitlements(List.of()).grant(command(transactionId, productId)).stream().toList();
	}

	private List<Entitlement> concat(List<Entitlement> first, List<Entitlement> second) {
		List<Entitlement> merged = new ArrayList<>(first);
		merged.addAll(second);
		return merged;
	}
}
