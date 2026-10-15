package com.daesabu.meongcoach.entitlement.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevenueCatGrantEntitlementRequest(
		@JsonProperty("entitlement_id") String entitlementId,
		@JsonProperty("expires_at") long expiresAt
) {
}
