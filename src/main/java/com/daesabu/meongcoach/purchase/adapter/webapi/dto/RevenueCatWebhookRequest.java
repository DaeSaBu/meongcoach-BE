package com.daesabu.meongcoach.purchase.adapter.webapi.dto;

import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * RevenueCat 웹훅 본문 중 구매 등록에 쓰는 부분만 담는다. 형식은 RevenueCat이 정하므로 provided Request로 바꿔 넘긴다.
 * 이벤트 타입마다 들어오는 필드가 달라(TRANSFER에는 app_user_id가 없다) type만 제약을 두고, 구매 값은 서비스가 검증한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(@Valid @NotNull Event event) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Event(
			@NotBlank String type,
			String id,
			@JsonProperty("app_user_id") String appUserId,
			@JsonProperty("transaction_id") String transactionId,
			@JsonProperty("product_id") String productId,
			String store,
			@JsonProperty("price_in_purchased_currency") BigDecimal price,
			String currency,
			@JsonProperty("purchased_at_ms") Long purchasedAtMs,
			@JsonProperty("entitlement_ids") Set<String> entitlementIds
	) {

		private static final String NON_RENEWING_PURCHASE = "NON_RENEWING_PURCHASE";

		// 앱이 RevenueCat에 로그인시킨 회원 ID. 로그인 전 구매는 $RCAnonymousID:로 시작하는 익명 ID로 온다.
		// 숫자가 아닌 ID를 Long으로 바꾸다 예외가 나면 RevenueCat이 재전송을 반복하므로 먼저 걸러 낸다. 18자리까지는 Long 범위를 넘지 않는다
		private static final Pattern MEMBER_APP_USER_ID = Pattern.compile("\\d{1,18}");

		// 현재 상품은 평생권(비갱신)뿐이라 이 타입만 구매로 등록한다
		public boolean isNonRenewingPurchase() {
			return NON_RENEWING_PURCHASE.equals(type);
		}

		public Optional<Long> findUserId() {
			if (appUserId == null || !MEMBER_APP_USER_ID.matcher(appUserId).matches()) {
				return Optional.empty();
			}
			Long userId = Long.valueOf(appUserId);
			return Optional.of(userId);
		}

		public PurchaseRegisterRequest toPurchaseRegisterRequest(Long userId) {
			Instant purchasedAt = purchasedAt();
			return new PurchaseRegisterRequest(userId, transactionId, productId, store, price, currency, purchasedAt,
					entitlementIds);
		}

		private Instant purchasedAt() {
			if (purchasedAtMs == null) {
				return null;
			}
			return Instant.ofEpochMilli(purchasedAtMs);
		}
	}
}
