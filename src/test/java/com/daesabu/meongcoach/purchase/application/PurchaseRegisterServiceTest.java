package com.daesabu.meongcoach.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.application.EntitlementGrantService;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.PurchaseRepository;
import com.daesabu.meongcoach.purchase.domain.Purchase;
import com.daesabu.meongcoach.user.application.UserQueryService;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PurchaseRegisterServiceTest {

	private static final String TRANSACTION_ID = "test_1790181485867_15EC5536-9647-4D92-B357-19A8D4495B5A";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private PurchaseRegisterService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new PurchaseRegisterService(new UserQueryService(userRepository), purchaseRepository,
				new EntitlementGrantService(entitlementRepository));
		userId = userRepository.save(User.registerUser()).getId();
	}

	@Test
	void 통합_상품을_구매하면_구매_한_건과_시기별_이용권_네_개가_저장된다() {
		service.register(request(userId, Set.of("puppy", "junior", "adult", "senior")));

		assertThat(purchaseRepository.findAll()).singleElement().satisfies(purchase -> {
			assertThat(purchase.getUserId()).isEqualTo(userId);
			assertThat(purchase.getTransactionId()).isEqualTo(TRANSACTION_ID);
			assertThat(purchase.getProductId()).isEqualTo("meongcoach_all_lifetime");
		});
		assertThat(entitlementRepository.findAll())
				.extracting(Entitlement::getIdentifier)
				.containsExactlyInAnyOrder("puppy", "junior", "adult", "senior");
	}

	@Test
	void 같은_거래가_다시_오면_구매와_이용권을_중복_저장하지_않는다() {
		PurchaseRegisterRequest request = request(userId, Set.of("puppy"));
		service.register(request);

		service.register(request);

		assertThat(purchaseRepository.findAll()).hasSize(1);
		assertThat(entitlementRepository.findAll()).hasSize(1);
	}

	@Test
	void 권한이_없는_상품이면_구매만_저장된다() {
		service.register(request(userId, null));

		assertThat(purchaseRepository.findAll()).extracting(Purchase::getUserId).containsExactly(userId);
		assertThat(entitlementRepository.findAll()).isEmpty();
	}

	@Test
	void 없는_회원의_구매는_저장하지_않는다() {
		Long unknownUserId = userId + 1000;

		service.register(request(unknownUserId, Set.of("puppy")));

		assertThat(purchaseRepository.findAll()).isEmpty();
		assertThat(entitlementRepository.findAll()).isEmpty();
	}

	private PurchaseRegisterRequest request(Long userId, Set<String> entitlementIds) {
		return new PurchaseRegisterRequest(userId, TRANSACTION_ID, "meongcoach_all_lifetime", "TEST_STORE",
				new BigDecimal("6.99"), "USD", Instant.parse("2026-09-23T16:38:06Z"), entitlementIds);
	}
}
