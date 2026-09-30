package com.daesabu.meongcoach.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import org.junit.jupiter.api.Test;

class EntitlementTest {

	private static final Long USER_ID = 1L;

	@Test
	void 활성_이용권을_회수하면_회수_시각이_남는다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);

		entitlement.revoke();

		assertThat(entitlement.isActive()).isFalse();
		assertThat(entitlement.getRevokedAt()).isNotNull();
	}

	@Test
	void 이미_회수된_이용권을_다시_회수하면_예외가_발생한다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		entitlement.revoke();

		assertThatThrownBy(entitlement::revoke)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 회수된_이용권을_복구하면_다시_활성이_된다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		entitlement.revoke();

		entitlement.restore();

		assertThat(entitlement.isActive()).isTrue();
		assertThat(entitlement.getRevokedAt()).isNull();
	}

	@Test
	void 활성_이용권을_복구하면_예외가_발생한다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);

		assertThatThrownBy(entitlement::restore)
				.isInstanceOf(IllegalStateException.class);
	}
}
