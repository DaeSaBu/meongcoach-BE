package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.application.provided.dto.EntitlementGrantRequest;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EntitlementGrantServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long PURCHASE_ID = 10L;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private EntitlementGrantService service;

	@BeforeEach
	void setUp() {
		service = new EntitlementGrantService(entitlementRepository);
	}

	@Test
	void 구매로_받은_권한_수만큼_이용권이_저장된다() {
		service.grant(new EntitlementGrantRequest(USER_ID, PURCHASE_ID, Set.of("puppy", "junior", "adult", "senior")));

		assertThat(entitlementRepository.findAll())
				.hasSize(4)
				.allSatisfy(entitlement -> {
					assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
					assertThat(entitlement.getPurchaseId()).isEqualTo(PURCHASE_ID);
				});
	}

	@Test
	void 받은_권한이_없으면_이용권을_저장하지_않는다() {
		service.grant(new EntitlementGrantRequest(USER_ID, PURCHASE_ID, Set.of()));

		assertThat(entitlementRepository.findAll()).isEmpty();
	}
}
