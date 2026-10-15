package com.daesabu.meongcoach.entitlement.adapter.integration;

import com.daesabu.meongcoach.entitlement.adapter.integration.dto.RevenueCatCreateCustomerRequest;
import com.daesabu.meongcoach.entitlement.adapter.integration.dto.RevenueCatGrantEntitlementRequest;
import com.daesabu.meongcoach.entitlement.application.required.PromotionalEntitlementGranter;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RevenueCat v2 고객 액션으로 프로모션 이용권을 지급한다.
 * 같은 이용권을 더 늦은 만료 시각으로 다시 지급하면 RevenueCat이 기존 지급을 연장한다(기존 만료보다 2시간 이상 뒤여야 함).
 * 앱에서 구매 SDK를 초기화한 적이 없어 고객이 없으면 404가 오므로, 그때는 고객을 만든 뒤 한 번 다시 지급한다.
 */
@Slf4j
@Component
public class RevenueCatEntitlementGranter implements PromotionalEntitlementGranter {

	private static final String GRANT_PATH = "/projects/{projectId}/customers/{customerId}/actions/grant_entitlement";
	private static final String CUSTOMERS_PATH = "/projects/{projectId}/customers";

	private final RevenueCatProperties properties;
	private final RestClient restClient;

	public RevenueCatEntitlementGranter(RevenueCatProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public void grant(Long userId, EntitlementType type, Instant expiresAt) {
		RevenueCatGrantEntitlementRequest request =
				new RevenueCatGrantEntitlementRequest(properties.entitlementIds().get(type), expiresAt.toEpochMilli());
		try {
			postGrant(userId, request);
		} catch (RestClientResponseException e) {
			if (!e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				throw new EntitlementProviderUnavailableException(e);
			}
			log.info("RevenueCat에 없는 고객이라 고객을 만든 뒤 다시 지급합니다: userId={}", userId);
			createCustomerAndRetry(userId, request);
		} catch (RestClientException e) {
			throw new EntitlementProviderUnavailableException(e);
		}
	}

	private void createCustomerAndRetry(Long userId, RevenueCatGrantEntitlementRequest request) {
		try {
			restClient.post()
					.uri(properties.baseUrl() + CUSTOMERS_PATH, properties.projectId())
					.headers(headers -> headers.setBearerAuth(properties.apiKey()))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new RevenueCatCreateCustomerRequest(String.valueOf(userId)))
					.retrieve()
					.toBodilessEntity();
			postGrant(userId, request);
		} catch (RestClientException e) {
			throw new EntitlementProviderUnavailableException(e);
		}
	}

	private void postGrant(Long userId, RevenueCatGrantEntitlementRequest request) {
		restClient.post()
				.uri(properties.baseUrl() + GRANT_PATH, properties.projectId(), userId)
				.headers(headers -> headers.setBearerAuth(properties.apiKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity();
	}
}
