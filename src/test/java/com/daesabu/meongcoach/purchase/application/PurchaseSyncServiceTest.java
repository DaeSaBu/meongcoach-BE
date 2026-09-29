package com.daesabu.meongcoach.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.application.EntitlementGrantService;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.PurchaseRepository;
import com.daesabu.meongcoach.purchase.domain.Purchase;
import com.daesabu.meongcoach.purchase.domain.Store;
import com.daesabu.meongcoach.user.application.UserQueryService;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PurchaseSyncServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	// 스토어가 돌려줄 소유 구매. 테스트마다 채운다
	private final List<PurchaseRegisterRequest> storePurchases = new ArrayList<>();

	private PurchaseSyncService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		PurchaseModifyService purchaseModifyService = new PurchaseModifyService(new UserQueryService(userRepository),
				purchaseRepository, new EntitlementGrantService(entitlementRepository));
		service = new PurchaseSyncService(requestedUserId -> List.copyOf(storePurchases), purchaseModifyService);
		userId = userRepository.save(User.registerUser()).getId();
	}

	@Test
	void 스토어에서_소유한_구매를_모두_등록하고_이용권을_부여한다() {
		storePurchases.add(request("2000000900000001", Set.of(EntitlementType.PUPPY)));
		storePurchases.add(request("2000000900000002", Set.of(EntitlementType.ADULT, EntitlementType.SENIOR)));

		service.synchronize(userId);

		assertThat(purchaseRepository.findAll())
				.extracting(Purchase::getTransactionId)
				.containsExactlyInAnyOrder("2000000900000001", "2000000900000002");
		assertThat(entitlementRepository.findAll())
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.PUPPY, EntitlementType.ADULT, EntitlementType.SENIOR);
	}

	@Test
	void 다시_동기화해도_이미_등록한_구매는_중복_저장하지_않는다() {
		storePurchases.add(request("2000000900000001", Set.of(EntitlementType.PUPPY)));
		service.synchronize(userId);

		service.synchronize(userId);

		assertThat(purchaseRepository.findAll()).hasSize(1);
		assertThat(entitlementRepository.findAll()).hasSize(1);
	}

	@Test
	void 스토어에_소유한_구매가_없으면_아무것도_저장하지_않는다() {
		service.synchronize(userId);

		assertThat(purchaseRepository.findAll()).isEmpty();
		assertThat(entitlementRepository.findAll()).isEmpty();
	}

	private PurchaseRegisterRequest request(String transactionId, Set<EntitlementType> entitlementTypes) {
		return new PurchaseRegisterRequest(userId, transactionId, Store.APP_STORE, new BigDecimal("6.99"), "USD",
				Instant.parse("2026-09-23T16:38:06Z"), entitlementTypes);
	}
}
