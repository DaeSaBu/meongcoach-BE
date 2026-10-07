package com.daesabu.meongcoach.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntitlementsTest {

	private static final Long USER_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
	private static final Instant NEXT_MONTH = NOW.plus(Duration.ofDays(30));
	private static final Instant TWO_MONTHS_LATER = NOW.plus(Duration.ofDays(60));
	private static final Instant LAST_MONTH = NOW.minus(Duration.ofDays(30));

	@Test
	void 가진_이용권이_없을_때_통합_상품의_네_종류가_오면_네_개를_새로_부여한다() {
		Entitlements entitlements = new Entitlements(List.of());
		List<ActiveEntitlement> activeEntitlements = Arrays.stream(EntitlementType.values())
				.map(type -> new ActiveEntitlement(type, null))
				.toList();

		List<Entitlement> granted = entitlements.synchronize(USER_ID, activeEntitlements, NOW);

		assertThat(granted)
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.values());
		assertThat(granted).allSatisfy(entitlement -> {
			assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
			assertThat(entitlement.isActive(NOW)).isTrue();
		});
	}

	@Test
	void 새로_부여한_이용권은_받은_만료_시각을_가진다() {
		Entitlements entitlements = new Entitlements(List.of());

		List<Entitlement> granted = entitlements.synchronize(USER_ID,
				List.of(new ActiveEntitlement(EntitlementType.PUPPY, NEXT_MONTH)), NOW);

		assertThat(granted)
				.singleElement()
				.extracting(Entitlement::getExpiresAt)
				.isEqualTo(NEXT_MONTH);
	}

	@Test
	void 이미_가진_종류는_새로_부여하지_않고_없던_종류만_부여한다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, null);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID, List.of(
				new ActiveEntitlement(EntitlementType.PUPPY, null),
				new ActiveEntitlement(EntitlementType.JUNIOR, null)
		), NOW);

		assertThat(granted).extracting(Entitlement::getType).containsExactly(EntitlementType.JUNIOR);
		assertThat(puppy.isActive(NOW)).isTrue();
	}

	@Test
	void 구독이_갱신되면_같은_이용권의_만료_시각을_늘린다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, NEXT_MONTH);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID,
				List.of(new ActiveEntitlement(EntitlementType.PUPPY, TWO_MONTHS_LATER)), NOW);

		assertThat(granted).isEmpty();
		assertThat(puppy.getExpiresAt()).isEqualTo(TWO_MONTHS_LATER);
	}

	@Test
	void 활성_목록에서_빠진_종류는_동기화_시각으로_만료시킨다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, null);
		Entitlement junior = Entitlement.grant(USER_ID, EntitlementType.JUNIOR, null);
		Entitlements entitlements = new Entitlements(List.of(puppy, junior));

		List<Entitlement> granted = entitlements.synchronize(USER_ID,
				List.of(new ActiveEntitlement(EntitlementType.JUNIOR, null)), NOW);

		assertThat(granted).isEmpty();
		assertThat(puppy.getExpiresAt()).isEqualTo(NOW);
		assertThat(puppy.isActive(NOW)).isFalse();
		assertThat(junior.isActive(NOW)).isTrue();
	}

	@Test
	void 활성_종류가_하나도_없으면_가진_이용권을_모두_만료시킨다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, null);
		Entitlement adult = Entitlement.grant(USER_ID, EntitlementType.ADULT, NEXT_MONTH);
		Entitlements entitlements = new Entitlements(List.of(puppy, adult));

		entitlements.synchronize(USER_ID, List.of(), NOW);

		assertThat(List.of(puppy, adult)).noneMatch(entitlement -> entitlement.isActive(NOW));
	}

	@Test
	void 이미_만료된_이용권은_만료_시각을_바꾸지_않는다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, LAST_MONTH);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		entitlements.synchronize(USER_ID, List.of(), NOW);

		assertThat(puppy.getExpiresAt()).isEqualTo(LAST_MONTH);
	}

	@Test
	void 만료된_종류가_다시_오면_새로_만들지_않고_되살린다() {
		Entitlement puppy = Entitlement.grant(USER_ID, EntitlementType.PUPPY, LAST_MONTH);
		Entitlements entitlements = new Entitlements(List.of(puppy));

		List<Entitlement> granted = entitlements.synchronize(USER_ID,
				List.of(new ActiveEntitlement(EntitlementType.PUPPY, NEXT_MONTH)), NOW);

		assertThat(granted).isEmpty();
		assertThat(puppy.getExpiresAt()).isEqualTo(NEXT_MONTH);
		assertThat(puppy.isActive(NOW)).isTrue();
	}
}
