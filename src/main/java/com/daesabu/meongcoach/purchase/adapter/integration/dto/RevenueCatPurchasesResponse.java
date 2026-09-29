package com.daesabu.meongcoach.purchase.adapter.integration.dto;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.domain.Store;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RevenueCat REST API v2 고객 구매 목록(GET /projects/{project_id}/customers/{customer_id}/purchases) 응답 중
 * 구매 등록에 쓰는 부분만 담는다. 어떤 구매를 등록할지는 RevenueCatStorePurchaseReader가 판단하고, 이 record는 값 형식 변환만 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatPurchasesResponse(
		List<Item> items,
		@JsonProperty("next_page") String nextPage
) {

	public boolean hasNextPage() {
		return nextPage != null;
	}

	/**
	 * product_id는 스토어 상품 ID가 아닌 RevenueCat 내부 ID라 받지 않는다. 무엇을 샀는지는 entitlements로 남는다.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Item(
			@JsonProperty("store_purchase_identifier") String storePurchaseIdentifier,
			String store,
			String status,
			@JsonProperty("purchased_at") Long purchasedAt,
			@JsonProperty("revenue_in_usd") Revenue revenueInUsd,
			EntitlementList entitlements
	) {

		private static final String OWNED = "owned";

		public boolean isOwned() {
			return OWNED.equals(status);
		}

		// 스토어와 모든 이용권을 우리가 알아야 등록할 수 있다. 하나라도 모르면 구매 전체를 등록하지 않는다
		public boolean isSupported() {
			return Store.isSupported(storeName()) && entitlements.lookupKeys().allMatch(EntitlementType::isSupported);
		}

		public List<String> lookupKeys() {
			return entitlements.lookupKeys().toList();
		}

		// isSupported()로 거른 뒤 호출한다. 모르는 스토어·이용권이면 예외로 등록을 거절한다
		public PurchaseRegisterRequest toPurchaseRegisterRequest(Long userId) {
			Store purchaseStore = Store.from(storeName());
			Instant purchasedTime = Instant.ofEpochMilli(purchasedAt);
			Set<EntitlementType> entitlementTypes = entitlements.toEntitlementTypes();
			return new PurchaseRegisterRequest(userId, storePurchaseIdentifier, purchaseStore, revenueInUsd.gross(),
					revenueInUsd.currency(), purchasedTime, entitlementTypes);
		}

		// 스토어는 소문자(app_store)로 오므로 Store 상수 이름으로 맞춘다
		private String storeName() {
			return store.toUpperCase(Locale.ROOT);
		}
	}

	/**
	 * 결제 통화가 아니라 USD로 환산한 매출이다. gross는 세금·스토어 수수료를 빼기 전 금액이고, 통화 코드도 응답 값 그대로 저장한다.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Revenue(String currency, BigDecimal gross) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EntitlementList(List<Entitlement> items) {

		// lookup_key가 RevenueCat 대시보드에 등록한 이용권 식별자(puppy 등)다. items의 id는 RevenueCat 내부 ID다
		Set<EntitlementType> toEntitlementTypes() {
			return lookupKeys()
					.map(EntitlementType::from)
					.collect(Collectors.toUnmodifiableSet());
		}

		Stream<String> lookupKeys() {
			return items.stream()
					.map(Entitlement::lookupKey);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entitlement(@JsonProperty("lookup_key") String lookupKey) {
	}
}
