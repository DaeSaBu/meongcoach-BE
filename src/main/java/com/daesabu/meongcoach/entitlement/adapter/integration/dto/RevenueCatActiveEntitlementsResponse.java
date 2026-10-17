package com.daesabu.meongcoach.entitlement.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatActiveEntitlementsResponse(List<Item> items) {

	public RevenueCatActiveEntitlementsResponse {
		items = Objects.requireNonNullElse(items, List.of());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Item(
			@JsonProperty("entitlement_id") String entitlementId,
			@JsonProperty("expires_at") Long expiresAt
	) {
	}
}
