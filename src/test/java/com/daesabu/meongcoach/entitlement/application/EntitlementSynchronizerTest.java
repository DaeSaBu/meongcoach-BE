package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class EntitlementSynchronizerTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private EntitlementSynchronizer entitlementSynchronizer;

	@Autowired
	private ActiveEntitlementReader activeEntitlementReader;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private static ActiveEntitlement lifetime(EntitlementType type) {
		return new ActiveEntitlement(type, null);
	}

	private static boolean isActiveNow(Entitlement entitlement) {
		return entitlement.isActive(Instant.now());
	}

	@Test
	void RevenueCat의_활성_이용권으로_회원의_이용권을_맞춘다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY), lifetime(EntitlementType.JUNIOR)));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, EntitlementSynchronizerTest::isActiveNow)
				.containsExactlyInAnyOrder(
						tuple(EntitlementType.PUPPY, true),
						tuple(EntitlementType.JUNIOR, true)
				);
	}

	@Test
	void 같은_활성_이용권으로_다시_맞춰도_행이_늘지_않는다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY)));
		entitlementSynchronizer.synchronize(USER_ID);

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getType)
				.isEqualTo(EntitlementType.PUPPY);
	}

	@Test
	void 구독이_갱신되면_같은_행의_만료_시각을_늘린다() {
		Instant firstExpiresAt = Instant.now().plus(Duration.ofDays(30));
		Instant renewedExpiresAt = firstExpiresAt.plus(Duration.ofDays(30));
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(new ActiveEntitlement(EntitlementType.PUPPY, firstExpiresAt)));
		entitlementSynchronizer.synchronize(USER_ID);
		Long puppyId = entitlementRepository.findAllByUserId(USER_ID).getFirst().getId();
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(new ActiveEntitlement(EntitlementType.PUPPY, renewedExpiresAt)));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getId, Entitlement::getExpiresAt)
				.containsExactly(puppyId, renewedExpiresAt);
	}

	@Test
	void RevenueCat에서_빠진_이용권은_행을_남긴_채_만료시킨다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY), lifetime(EntitlementType.JUNIOR)));
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.JUNIOR)));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, EntitlementSynchronizerTest::isActiveNow)
				.containsExactlyInAnyOrder(
						tuple(EntitlementType.PUPPY, false),
						tuple(EntitlementType.JUNIOR, true)
				);
	}

	@Test
	void 만료된_이용권이_다시_활성이_되면_같은_행을_되살린다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY)));
		entitlementSynchronizer.synchronize(USER_ID);
		Long puppyId = entitlementRepository.findAllByUserId(USER_ID).getFirst().getId();
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of());
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY)));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getId, EntitlementSynchronizerTest::isActiveNow)
				.containsExactly(puppyId, true);
	}

	@Test
	void 다른_회원의_이용권은_건드리지_않는다() {
		given(activeEntitlementReader.readActiveEntitlements(OTHER_USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY)));
		entitlementSynchronizer.synchronize(OTHER_USER_ID);
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of());

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(OTHER_USER_ID))
				.extracting(Entitlement::getType, EntitlementSynchronizerTest::isActiveNow)
				.containsExactly(tuple(EntitlementType.PUPPY, true));
	}

	@Test
	void RevenueCat에서_이용권을_받아_오지_못하면_기존_이용권을_만료시키지_않는다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of(lifetime(EntitlementType.PUPPY)));
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willThrow(new EntitlementProviderUnavailableException());

		assertThatThrownBy(() -> entitlementSynchronizer.synchronize(USER_ID))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, EntitlementSynchronizerTest::isActiveNow)
				.containsExactly(tuple(EntitlementType.PUPPY, true));
	}

	@Test
	void 요청한_회원으로_RevenueCat의_활성_이용권을_조회한다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID))
				.willReturn(List.of());

		entitlementSynchronizer.synchronize(USER_ID);

		verify(activeEntitlementReader).readActiveEntitlements(USER_ID);
	}
}
