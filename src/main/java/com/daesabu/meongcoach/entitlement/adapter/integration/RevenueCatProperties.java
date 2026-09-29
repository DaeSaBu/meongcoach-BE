package com.daesabu.meongcoach.entitlement.adapter.integration;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RevenueCat REST API v2 연동 설정. 값이 없으면 첫 동기화가 아니라 기동 시점에 실패하도록 모든 필드에 제약을 건다.
 *
 * @param baseUrl        REST API v2 베이스 URL
 * @param apiKey         v2 secret API 키. Bearer 토큰으로 전송한다. 활성 이용권 조회만 하므로 customer_information:customers:read 권한만 준다
 * @param projectId      이용권을 조회할 RevenueCat 프로젝트 ID (proj로 시작)
 * @param entitlementIds 이용권 종류별 RevenueCat entitlement ID(entl로 시작). 활성 이용권 응답이 lookup_key 없이 이 ID만 주기 때문에 둔다
 */
@Validated
@ConfigurationProperties("meongcoach.revenuecat")
public record RevenueCatProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotBlank String projectId,
		Map<EntitlementType, String> entitlementIds
) {

	// ID가 빠진 종류는 동기화할 때마다 활성 목록에 없는 것으로 보여 회수되므로 기동을 막는다
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
