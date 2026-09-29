package com.daesabu.meongcoach.entitlement.adapter.integration;

import com.daesabu.meongcoach.entitlement.adapter.integration.dto.RevenueCatActiveEntitlementsResponse;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RevenueCat REST API v2로 회원의 활성 이용권을 조회한다. 앱은 결제 전에 회원 ID로 RevenueCat에 로그인하므로
 * RevenueCat 고객 ID(app_user_id)가 곧 회원 ID다. 이용권 종류가 넷뿐이라 한 페이지(최대 100건)만 읽는다.
 */
@Slf4j
@Component
public class RevenueCatActiveEntitlementReader implements ActiveEntitlementReader {

	private static final String ACTIVE_ENTITLEMENTS_PATH =
			"/projects/{projectId}/customers/{customerId}/active_entitlements?limit={limit}";
	private static final int PAGE_LIMIT = 100;

	private final RevenueCatProperties properties;
	private final RestClient restClient;

	public RevenueCatActiveEntitlementReader(RevenueCatProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public Set<EntitlementType> readActiveTypes(Long userId) {
		RevenueCatActiveEntitlementsResponse response = fetchActiveEntitlements(userId);
		if (response == null) {
			return Set.of();
		}
		return response.entitlementIds().stream()
				.map(entitlementId -> toEntitlementType(entitlementId, userId))
				.flatMap(Optional::stream)
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * 대시보드에 우리 enum·설정보다 먼저 추가한 이용권은 바꿀 종류가 없어 무시한다. 활성 종류 집합으로 맞추므로 다른 이용권에는 영향이 없고,
	 * enum과 설정을 배포한 뒤 다시 동기화하면 부여된다.
	 */
	private Optional<EntitlementType> toEntitlementType(String entitlementId, Long userId) {
		Optional<EntitlementType> entitlementType = properties.findEntitlementType(entitlementId);
		if (entitlementType.isEmpty()) {
			log.error("설정에 없는 RevenueCat 이용권이라 무시했습니다: userId={}, entitlementId={}", userId, entitlementId);
		}
		return entitlementType;
	}

	private RevenueCatActiveEntitlementsResponse fetchActiveEntitlements(Long userId) {
		try {
			return restClient.get()
					.uri(properties.baseUrl() + ACTIVE_ENTITLEMENTS_PATH, properties.projectId(), userId, PAGE_LIMIT)
					.headers(headers -> headers.setBearerAuth(properties.apiKey()))
					.retrieve()
					.body(RevenueCatActiveEntitlementsResponse.class);
		} catch (RestClientResponseException e) {
			// RevenueCat 고객은 SDK가 처음 로그인할 때 생긴다. 고객이 없으면 이용권도 없는 것이다
			if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				return null;
			}
			throw new EntitlementProviderUnavailableException(e);
		} catch (RestClientException e) {
			// 연결·타임아웃 실패다. 이용권이 없는 것과 구분해야 회수하지 않고 클라이언트가 재시도할 수 있다
			throw new EntitlementProviderUnavailableException(e);
		}
	}
}
