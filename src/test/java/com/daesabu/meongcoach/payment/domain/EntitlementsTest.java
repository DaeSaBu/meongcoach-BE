package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntitlementsTest {

	private static final Long USER_ID = 1L;
	private static final Instant GRANTED_AT = Instant.parse("2026-09-24T03:00:00Z");
	private static final Instant REVOKED_AT = Instant.parse("2026-09-25T03:00:00Z");

	@Test
	void 퍼피_이용권을_사면_PUPPY_이용권_하나가_생긴다() {
		Entitlements entitlements = new Entitlements(List.of());

		List<Entitlement> granted = entitlements.grant(command(ProductId.PUPPY_LIFETIME));

		assertThat(granted).extracting(Entitlement::getLifeStage)
				.containsExactly(LifeStage.PUPPY);
	}

	@Test
	void 통합_이용권을_사면_네_시기의_이용권이_생긴다() {
		Entitlements entitlements = new Entitlements(List.of());

		List<Entitlement> granted = entitlements.grant(command(ProductId.ALL_LIFETIME));

		assertThat(granted).extracting(Entitlement::getLifeStage)
				.containsExactlyInAnyOrder(LifeStage.PUPPY, LifeStage.JUNIOR, LifeStage.ADULT, LifeStage.SENIOR);
		assertThat(granted).extracting(Entitlement::getProductId)
				.containsOnly(ProductId.ALL_LIFETIME);
	}

	@Test
	void 이미_가진_시기라도_통합_이용권을_사면_그_시기의_이용권을_새로_만든다() {
		Entitlements entitlements = new Entitlements(owned(ProductId.PUPPY_LIFETIME));

		List<Entitlement> granted = entitlements.grant(command(ProductId.ALL_LIFETIME));

		assertThat(granted).extracting(Entitlement::getLifeStage)
				.contains(LifeStage.PUPPY);
	}

	@Test
	void 활성_이용권이_있는_시기는_권한이_있다() {
		Entitlements entitlements = new Entitlements(owned(ProductId.PUPPY_LIFETIME));

		boolean hasAccess = entitlements.hasAccess(LifeStage.PUPPY);

		assertThat(hasAccess).isTrue();
	}

	@Test
	void 이용권이_없는_시기는_권한이_없다() {
		Entitlements entitlements = new Entitlements(owned(ProductId.PUPPY_LIFETIME));

		boolean hasAccess = entitlements.hasAccess(LifeStage.ADULT);

		assertThat(hasAccess).isFalse();
	}

	@Test
	void 회수된_이용권만_있는_시기는_권한이_없다() {
		Entitlements entitlements = new Entitlements(owned(ProductId.PUPPY_LIFETIME));
		entitlements.revoke(ProductId.PUPPY_LIFETIME, REVOKED_AT);

		boolean hasAccess = entitlements.hasAccess(LifeStage.PUPPY);

		assertThat(hasAccess).isFalse();
	}

	@Test
	void 상품을_회수하면_그_상품으로_받은_이용권만_회수된다() {
		List<Entitlement> puppy = owned(ProductId.PUPPY_LIFETIME);
		List<Entitlement> all = owned(ProductId.ALL_LIFETIME);
		Entitlements entitlements = new Entitlements(concat(puppy, all));

		entitlements.revoke(ProductId.PUPPY_LIFETIME, REVOKED_AT);

		assertThat(puppy).allMatch(entitlement -> entitlement.getRevokedAt().equals(REVOKED_AT));
		assertThat(all).allMatch(Entitlement::isActive);
	}

	@Test
	void 개별_이용권을_환불해도_통합_이용권이_있으면_그_시기_권한이_유지된다() {
		Entitlements entitlements = new Entitlements(
				concat(owned(ProductId.PUPPY_LIFETIME), owned(ProductId.ALL_LIFETIME))
		);

		entitlements.revoke(ProductId.PUPPY_LIFETIME, REVOKED_AT);

		assertThat(entitlements.hasAccess(LifeStage.PUPPY)).isTrue();
	}

	@Test
	void 가진_적_없는_상품을_회수해도_아무것도_바뀌지_않는다() {
		List<Entitlement> puppy = owned(ProductId.PUPPY_LIFETIME);
		Entitlements entitlements = new Entitlements(puppy);

		entitlements.revoke(ProductId.ALL_LIFETIME, REVOKED_AT);

		assertThat(puppy).allMatch(Entitlement::isActive);
	}

	private EntitlementGrantCommand command(ProductId productId) {
		return new EntitlementGrantCommand(USER_ID, productId, Store.APP_STORE, GRANTED_AT);
	}

	private List<Entitlement> owned(ProductId productId) {
		return new Entitlements(List.of()).grant(command(productId));
	}

	private List<Entitlement> concat(List<Entitlement> first, List<Entitlement> second) {
		List<Entitlement> merged = new ArrayList<>(first);
		merged.addAll(second);
		return merged;
	}
}
