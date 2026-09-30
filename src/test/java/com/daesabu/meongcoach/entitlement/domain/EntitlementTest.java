package com.daesabu.meongcoach.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

// 회수·복구가 필요한 행만 고르는 일은 Entitlements가 하므로, 같은 상태로 다시 바꾸려는 호출은 그 판단이 틀렸다는 뜻이다
class EntitlementTest {

	private static final Long USER_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");

	@Test
	void 활성_이용권을_회수하면_회수_시각이_남는다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);

		entitlement.revoke(NOW);

		assertThat(entitlement.isActive()).isFalse();
		assertThat(entitlement.getRevokedAt()).isEqualTo(NOW);
	}

	@Test
	void 이미_회수된_이용권을_다시_회수하면_예외가_발생한다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		entitlement.revoke(NOW);

		assertThatThrownBy(() -> entitlement.revoke(NOW))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 회수된_이용권을_복구하면_다시_활성이_된다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		entitlement.revoke(NOW);

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
