package com.daesabu.meongcoach.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntitlementsTest {

	private static final Long USER_ID = 1L;

	@Test
	void 가진_이용권이_없을_때_통합_상품의_네_종류가_오면_네_개를_새로_부여한다() {
		Entitlements entitlements = new Entitlements(List.of());

		List<Entitlement> granted = entitlements.synchronize(USER_ID, EnumSet.allOf(EntitlementType.class));

		assertThat(granted)
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.values());
		assertThat(granted).allSatisfy(entitlement -> {
			assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
			assertThat(entitlement.isActive()).isTrue();
		});
	}

	@Test
	void 이미_가진_종류는_새로_부여하지_않고_없던_종류만_부여한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID,
				Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR));

		assertThat(granted).extracting(Entitlement::getType).containsExactly(EntitlementType.JUNIOR);
		assertThat(puppy.isActive()).isTrue();
	}

	@Test
	void RevenueCat에서_빠진_종류는_회수한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		Entitlement junior = Entitlement.grant(USER_ID, EntitlementType.JUNIOR);
		Entitlements entitlements = new Entitlements(List.of(puppy, junior));

		List<Entitlement> granted = entitlements.synchronize(USER_ID, Set.of(EntitlementType.JUNIOR));

		assertThat(granted).isEmpty();
		assertThat(puppy.isActive()).isFalse();
		assertThat(puppy.getRevokedAt()).isNotNull();
		assertThat(junior.isActive()).isTrue();
	}

	@Test
	void 활성_종류가_하나도_없으면_가진_이용권을_모두_회수한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		Entitlement adult = Entitlement.grant(USER_ID, EntitlementType.ADULT);
		Entitlements entitlements = new Entitlements(List.of(puppy, adult));

		entitlements.synchronize(USER_ID, Set.of());

		assertThat(List.of(puppy, adult)).noneMatch(Entitlement::isActive);
	}

	@Test
	void 이미_회수된_이용권은_회수_시각을_바꾸지_않는다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		puppy.revoke();
		Instant firstRevokedAt = puppy.getRevokedAt();
		Entitlements entitlements = new Entitlements(List.of(puppy));

		entitlements.synchronize(USER_ID, Set.of());

		assertThat(puppy.getRevokedAt()).isEqualTo(firstRevokedAt);
	}

	@Test
	void 회수된_종류가_다시_오면_새로_만들지_않고_되살린다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		puppy.revoke();
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID, Set.of(EntitlementType.PUPPY));

		assertThat(granted).isEmpty();
		assertThat(puppy.isActive()).isTrue();
		assertThat(puppy.getRevokedAt()).isNull();
	}
}
