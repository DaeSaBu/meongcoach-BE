package com.daesabu.meongcoach.purchase.adapter.webapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.adapter.webapi.dto.RevenueCatWebhookRequest;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.domain.Store;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RevenueCatPurchaseTranslatorTest {

	private static final String NON_RENEWING_PURCHASE = "NON_RENEWING_PURCHASE";
	private static final String TRANSACTION_ID = "2000000912345678";
	private static final long PURCHASED_AT_MS = 1790181485867L;

	private final RevenueCatPurchaseTranslator translator = new RevenueCatPurchaseTranslator();

	@Test
	void 평생권_구매_이벤트면_회원_ID로_구매_등록_요청을_만든다() {
		RevenueCatWebhookRequest.Event event = event(NON_RENEWING_PURCHASE, "42");

		Optional<PurchaseRegisterRequest> request = translator.translate(event);

		assertThat(request).contains(new PurchaseRegisterRequest(42L, TRANSACTION_ID, "meongcoach_all_lifetime",
				Store.APP_STORE, new BigDecimal("6.99"), "USD", Instant.ofEpochMilli(PURCHASED_AT_MS),
				Set.of(EntitlementType.PUPPY)));
	}

	@Test
	void 처리하지_않는_이벤트_타입이면_요청을_만들지_않는다() {
		RevenueCatWebhookRequest.Event event = event("RENEWAL", "42");

		assertThat(translator.translate(event)).isEmpty();
	}

	@Test
	void 익명_사용자의_구매면_요청을_만들지_않는다() {
		RevenueCatWebhookRequest.Event event = event(NON_RENEWING_PURCHASE, "$RCAnonymousID:8f3b2c1d");

		assertThat(translator.translate(event)).isEmpty();
	}

	@Test
	void 회원_ID가_Long_범위를_넘을_수_있는_19자리면_요청을_만들지_않는다() {
		RevenueCatWebhookRequest.Event event = event(NON_RENEWING_PURCHASE, "9999999999999999999");

		assertThat(translator.translate(event)).isEmpty();
	}

	@Test
	void 사용자_ID가_없으면_요청을_만들지_않는다() {
		RevenueCatWebhookRequest.Event event = event(NON_RENEWING_PURCHASE, null);

		assertThat(translator.translate(event)).isEmpty();
	}

	private static RevenueCatWebhookRequest.Event event(String type, String appUserId) {
		return new RevenueCatWebhookRequest.Event(type, "CD489E0E-5D2F-4F1B-9B7A-4C3E2A1B0F9D", appUserId,
				TRANSACTION_ID, "meongcoach_all_lifetime", "APP_STORE", new BigDecimal("6.99"), "USD",
				PURCHASED_AT_MS, Set.of("puppy"));
	}
}
