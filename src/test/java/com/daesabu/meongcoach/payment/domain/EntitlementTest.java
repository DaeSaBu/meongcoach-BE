package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntitlementTest {

	private static final Long USER_ID = 1L;
	private static final Instant GRANTED_AT = Instant.parse("2026-09-24T03:00:00Z");
	private static final Instant REVOKED_AT = Instant.parse("2026-09-25T03:00:00Z");

	@Test
	void 부여하면_회원과_시기와_상품과_스토어와_부여_시각이_담기고_활성_상태다() {
		EntitlementGrantCommand command = new EntitlementGrantCommand(
				USER_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, GRANTED_AT
		);

		Entitlement entitlement = Entitlement.grant(LifeStage.PUPPY, command);

		assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
		assertThat(entitlement.getLifeStage()).isEqualTo(LifeStage.PUPPY);
		assertThat(entitlement.getProductId()).isEqualTo(ProductId.PUPPY_LIFETIME);
		assertThat(entitlement.getStore()).isEqualTo(Store.APP_STORE);
		assertThat(entitlement.getGrantedAt()).isEqualTo(GRANTED_AT);
		assertThat(entitlement.getRevokedAt()).isNull();
		assertThat(entitlement.isActive()).isTrue();
	}

	@Test
	void 통합_이용권은_어느_시기로든_부여할_수_있다() {
		EntitlementGrantCommand command = new EntitlementGrantCommand(
				USER_ID, ProductId.ALL_LIFETIME, Store.PLAY_STORE, GRANTED_AT
		);

		Entitlement entitlement = Entitlement.grant(LifeStage.SENIOR, command);

		assertThat(entitlement.getLifeStage()).isEqualTo(LifeStage.SENIOR);
	}

	@Test
	void 상품이_주지_않는_시기로는_부여할_수_없다() {
		EntitlementGrantCommand command = new EntitlementGrantCommand(
				USER_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, GRANTED_AT
		);

		assertThatThrownBy(() -> Entitlement.grant(LifeStage.ADULT, command))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 회수하면_revokedAt이_기록되고_비활성_상태가_된다() {
		Entitlement entitlement = Entitlement.grant(
				LifeStage.PUPPY,
				new EntitlementGrantCommand(USER_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, GRANTED_AT)
		);

		entitlement.revoke(REVOKED_AT);

		assertThat(entitlement.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(entitlement.isActive()).isFalse();
	}

	@Test
	void 이미_회수된_이용권을_다시_회수해도_처음_시각이_유지된다() {
		Entitlement entitlement = Entitlement.grant(
				LifeStage.PUPPY,
				new EntitlementGrantCommand(USER_ID, ProductId.PUPPY_LIFETIME, Store.APP_STORE, GRANTED_AT)
		);
		entitlement.revoke(REVOKED_AT);

		entitlement.revoke(REVOKED_AT.plusSeconds(60));

		assertThat(entitlement.getRevokedAt()).isEqualTo(REVOKED_AT);
	}
}
