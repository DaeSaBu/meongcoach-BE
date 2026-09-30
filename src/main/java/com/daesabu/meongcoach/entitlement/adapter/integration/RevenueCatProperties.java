package com.daesabu.meongcoach.entitlement.adapter.integration;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.revenuecat")
public record RevenueCatProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotBlank String projectId,
		Map<EntitlementType, String> entitlementIds
) {

	@AssertTrue(message = "모든 이용권 종류의 RevenueCat entitlement ID가 필요합니다")
	public boolean isEntitlementIdsComplete() {
		if (entitlementIds == null) {
			return false;
		}
		return Arrays.stream(EntitlementType.values())
				.map(entitlementIds::get)
				.allMatch(entitlementId -> entitlementId != null && !entitlementId.isBlank());
	}

	public Optional<EntitlementType> findEntitlementType(String entitlementId) {
		return entitlementIds.entrySet().stream()
				.filter(entry -> entry.getValue().equals(entitlementId))
				.map(Map.Entry::getKey)
				.findFirst();
	}
}
