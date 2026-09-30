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
	private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-09-01T12:00:00Z");

	@Test
	void 가진_이용권이_없을_때_통합_상품의_네_종류가_오면_네_개를_새로_부여한다() {
		Entitlements entitlements = new Entitlements(List.of());

		List<Entitlement> granted = entitlements.synchronize(USER_ID, EnumSet.allOf(EntitlementType.class), NOW);

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
				Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR), NOW);

		assertThat(granted).extracting(Entitlement::getType).containsExactly(EntitlementType.JUNIOR);
		assertThat(puppy.isActive()).isTrue();
	}

	@Test
	void RevenueCat에서_빠진_종류는_회수한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		Entitlement junior = Entitlement.grant(USER_ID, EntitlementType.JUNIOR);
		Entitlements entitlements = new Entitlements(List.of(puppy, junior));

		List<Entitlement> granted = entitlements.synchronize(USER_ID, Set.of(EntitlementType.JUNIOR), NOW);

		assertThat(granted).isEmpty();
		assertThat(puppy.isActive()).isFalse();
		assertThat(puppy.getRevokedAt()).isEqualTo(NOW);
		assertThat(junior.isActive()).isTrue();
	}

	@Test
	void 활성_종류가_하나도_없으면_가진_이용권을_모두_회수한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		Entitlement adult = Entitlement.grant(USER_ID, EntitlementType.ADULT);
		Entitlements entitlements = new Entitlements(List.of(puppy, adult));

		entitlements.synchronize(USER_ID, Set.of(), NOW);

		assertThat(List.of(puppy, adult)).noneMatch(Entitlement::isActive);
	}

	// 회수 시각은 처음 회수를 알게 된 때다. 다시 동기화해도 바뀌지 않아야 언제 잃었는지 답할 수 있다
	@Test
	void 이미_회수된_이용권은_회수_시각을_바꾸지_않는다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		puppy.revoke(EARLIER);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		entitlements.synchronize(USER_ID, Set.of(), NOW);

		assertThat(puppy.getRevokedAt()).isEqualTo(EARLIER);
	}

	// 환불 뒤 재구매하거나 다른 계정으로 옮겨 갔던 구매를 복원하면 같은 종류가 다시 활성으로 온다
	@Test
	void 회수된_종류가_다시_오면_새로_만들지_않고_되살린다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY);
		puppy.revoke(EARLIER);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID, Set.of(EntitlementType.PUPPY), NOW);

		assertThat(granted).isEmpty();
		assertThat(puppy.isActive()).isTrue();
		assertThat(puppy.getRevokedAt()).isNull();
	}
}
