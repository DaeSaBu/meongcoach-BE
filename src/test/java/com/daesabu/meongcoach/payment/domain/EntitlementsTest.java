package com.daesabu.meongcoach.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntitlementsTest {

	private static final Long USER_ID = 1L;
	private static final Long PURCHASE_ID = 10L;

	@Test
	void 통합_상품_구매로_네_개의_권한이_오면_네_개의_이용권이_부여된다() {
		Purchase purchase = savedPurchase("meongcoach_all_lifetime");

		List<Entitlement> granted = Entitlements.grant(purchase, Set.of("puppy", "junior", "adult", "senior")).toList();

		assertThat(granted)
				.extracting(Entitlement::getIdentifier)
				.containsExactlyInAnyOrder("puppy", "junior", "adult", "senior");
	}

	@Test
	void 권한이_없는_구매면_이용권이_부여되지_않는다() {
		Purchase purchase = savedPurchase("meongcoach_ai_report_5");

		List<Entitlement> granted = Entitlements.grant(purchase, Set.of()).toList();

		assertThat(granted).isEmpty();
	}

	@Test
	void 부여된_이용권은_구매한_회원과_구매를_가리키고_회수되지_않은_상태다() {
		Purchase purchase = savedPurchase("meongcoach_puppy_lifetime");

		List<Entitlement> granted = Entitlements.grant(purchase, Set.of("puppy")).toList();

		assertThat(granted).singleElement().satisfies(entitlement -> {
			assertThat(entitlement.getUserId()).isEqualTo(USER_ID);
			assertThat(entitlement.getPurchaseId()).isEqualTo(PURCHASE_ID);
			assertThat(entitlement.getRevokedAt()).isNull();
		});
	}

	private Purchase savedPurchase(String productId) {
		PurchaseRegisterCommand command = new PurchaseRegisterCommand(
				"170000869511114", productId, "APP_STORE", new BigDecimal("9900"), "KRW",
				Instant.parse("2026-09-24T03:00:00Z")
		);
		Purchase purchase = Purchase.register(USER_ID, command);
		ReflectionTestUtils.setField(purchase, "id", PURCHASE_ID);
		return purchase;
	}
}
