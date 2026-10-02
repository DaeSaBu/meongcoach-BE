package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.util.Set;
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

	@Test
	void RevenueCat의_활성_이용권으로_회원의_이용권을_맞춘다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, Entitlement::isActive)
				.containsExactlyInAnyOrder(
						tuple(EntitlementType.PUPPY, true),
						tuple(EntitlementType.JUNIOR, true)
				);
	}

	@Test
	void 같은_활성_이용권으로_다시_맞춰도_행이_늘지_않는다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY));
		entitlementSynchronizer.synchronize(USER_ID);

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getType)
				.isEqualTo(EntitlementType.PUPPY);
	}

	@Test
	void RevenueCat에서_빠진_이용권은_행을_남긴_채_회수한다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR));
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.JUNIOR));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, Entitlement::isActive)
				.containsExactlyInAnyOrder(
						tuple(EntitlementType.PUPPY, false),
						tuple(EntitlementType.JUNIOR, true)
				);
	}

	@Test
	void 회수된_이용권이_다시_활성이_되면_같은_행을_복구한다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY));
		entitlementSynchronizer.synchronize(USER_ID);
		Long puppyId = entitlementRepository.findAllByUserId(USER_ID).getFirst().getId();
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of());
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY));

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.singleElement()
				.extracting(Entitlement::getId, Entitlement::isActive)
				.containsExactly(puppyId, true);
	}

	@Test
	void 다른_회원의_이용권은_건드리지_않는다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(OTHER_USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY));
		entitlementSynchronizer.synchronize(OTHER_USER_ID);
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of());

		entitlementSynchronizer.synchronize(USER_ID);

		assertThat(entitlementRepository.findAllByUserId(OTHER_USER_ID))
				.extracting(Entitlement::getType, Entitlement::isActive)
				.containsExactly(tuple(EntitlementType.PUPPY, true));
	}

	@Test
	void RevenueCat에서_이용권을_받아_오지_못하면_기존_이용권을_회수하지_않는다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of(EntitlementType.PUPPY));
		entitlementSynchronizer.synchronize(USER_ID);
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willThrow(new EntitlementProviderUnavailableException());

		assertThatThrownBy(() -> entitlementSynchronizer.synchronize(USER_ID))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, Entitlement::isActive)
				.containsExactly(tuple(EntitlementType.PUPPY, true));
	}

	@Test
	void 요청한_회원으로_RevenueCat의_활성_이용권을_조회한다() {
		given(activeEntitlementReader.readActiveEntitlementTypes(USER_ID))
				.willReturn(Set.of());

		entitlementSynchronizer.synchronize(USER_ID);

		verify(activeEntitlementReader).readActiveEntitlementTypes(USER_ID);
	}
}
