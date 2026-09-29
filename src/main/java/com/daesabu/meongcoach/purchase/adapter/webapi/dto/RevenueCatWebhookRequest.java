package com.daesabu.meongcoach.purchase.adapter.webapi.dto;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.domain.Store;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RevenueCat 웹훅 본문 중 구매 등록에 쓰는 부분만 담는다. 형식은 RevenueCat이 정하므로 provided Request로 바꿔 넘긴다.
 * 이벤트 타입마다 들어오는 필드가 달라(TRANSFER에는 app_user_id가 없다) type만 제약을 두고, 구매 값은 서비스가 검증한다.
 * 어떤 이벤트를 구매로 등록할지는 RevenueCatPurchaseTranslator가 판단하고, 이 record는 값 형식 변환만 한다.
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

		// 스토어·entitlement 식별자를 enum으로 바꾸고, 모르는 값이면 예외로 등록을 거절한다
		public PurchaseRegisterRequest toPurchaseRegisterRequest(Long userId) {
			Store purchaseStore = Store.from(store);
			Set<EntitlementType> entitlementTypes = entitlementTypes();
			Instant purchasedAt = purchasedAt();
			return new PurchaseRegisterRequest(userId, transactionId, productId, purchaseStore, price, currency,
					purchasedAt, entitlementTypes);
		}

		private Set<EntitlementType> entitlementTypes() {
			if (entitlementIds == null) {
				return Set.of();
			}
			return entitlementIds.stream()
					.map(EntitlementType::from)
					.collect(Collectors.toUnmodifiableSet());
		}

		private Instant purchasedAt() {
			if (purchasedAtMs == null) {
				return null;
			}
			return Instant.ofEpochMilli(purchasedAtMs);
		}
	}
}
