package com.daesabu.meongcoach.entitlement.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * RevenueCat REST API v2 고객 활성 이용권 목록(GET /projects/{project_id}/customers/{customer_id}/active_entitlements) 응답 중
 * 이용권 식별에 쓰는 부분만 담는다. 판매 상품이 평생 이용권뿐이라 만료 시각(expires_at)은 받지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatActiveEntitlementsResponse(List<Item> items) {

	public List<String> entitlementIds() {
		if (items == null) {
			return List.of();
		}
		return items.stream()
				.map(Item::entitlementId)
				.toList();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Item(@JsonProperty("entitlement_id") String entitlementId) {
	}
}
