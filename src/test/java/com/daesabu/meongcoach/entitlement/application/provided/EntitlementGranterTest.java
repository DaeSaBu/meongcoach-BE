package com.daesabu.meongcoach.entitlement.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.application.required.PromotionalEntitlementGranter;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class EntitlementGranterTest {

	private static final Long USER_ID = 42L;
	private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T05:03:00Z");

	@Autowired
	private EntitlementGranter entitlementGranter;

	@Autowired
	private PromotionalEntitlementGranter promotionalEntitlementGranter;

	@Autowired
	private ActiveEntitlementReader activeEntitlementReader;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Test
	void 네_종류_이용권을_같은_만료_시각으로_지급한_뒤_동기화해_서버_DB에_반영한다() {
		given(activeEntitlementReader.readActiveEntitlements(USER_ID)).willReturn(Arrays.stream(EntitlementType.values())
				.map(type -> new ActiveEntitlement(type, EXPIRES_AT))
				.toList());

		entitlementGranter.grant(USER_ID, EXPIRES_AT);

		for (EntitlementType type : EntitlementType.values()) {
			then(promotionalEntitlementGranter).should().grant(USER_ID, type, EXPIRES_AT);
		}
		assertThat(entitlementRepository.findAllByUserId(USER_ID))
				.extracting(Entitlement::getType, Entitlement::getExpiresAt)
				.containsExactlyInAnyOrder(
						tuple(EntitlementType.PUPPY, EXPIRES_AT),
						tuple(EntitlementType.JUNIOR, EXPIRES_AT),
						tuple(EntitlementType.ADULT, EXPIRES_AT),
						tuple(EntitlementType.SENIOR, EXPIRES_AT)
				);
	}

	@Test
	void 지급_도중_실패하면_예외를_전파하고_남은_종류는_지급하지_않는다() {
		willThrow(new EntitlementProviderUnavailableException())
				.given(promotionalEntitlementGranter).grant(eq(USER_ID), eq(EntitlementType.JUNIOR), any());

		assertThatThrownBy(() -> entitlementGranter.grant(USER_ID, EXPIRES_AT))
				.isInstanceOf(EntitlementProviderUnavailableException.class);

		then(promotionalEntitlementGranter).should(times(2)).grant(eq(USER_ID), any(), eq(EXPIRES_AT));
		then(activeEntitlementReader).shouldHaveNoInteractions();
		assertThat(entitlementRepository.findAllByUserId(USER_ID)).isEmpty();
	}
}
