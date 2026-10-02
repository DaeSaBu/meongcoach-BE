package com.daesabu.meongcoach.entitlement.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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
