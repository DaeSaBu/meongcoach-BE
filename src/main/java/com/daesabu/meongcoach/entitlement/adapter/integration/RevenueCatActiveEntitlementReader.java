package com.daesabu.meongcoach.entitlement.adapter.integration;

import com.daesabu.meongcoach.entitlement.adapter.integration.dto.RevenueCatActiveEntitlementsResponse;
import com.daesabu.meongcoach.entitlement.adapter.integration.dto.RevenueCatActiveEntitlementsResponse.Item;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.ActiveEntitlement;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
	public List<ActiveEntitlement> readActiveEntitlements(Long userId) {
		RevenueCatActiveEntitlementsResponse response = fetchActiveEntitlements(userId);
		if (response == null) {
			return List.of();
		}
		return response.items().stream()
				.map(item -> toActiveEntitlement(item, userId))
				.flatMap(Optional::stream)
				.toList();
	}

	private Optional<ActiveEntitlement> toActiveEntitlement(Item item, Long userId) {
		Instant expiresAt = toInstant(item.expiresAt());

		return toEntitlementType(item.entitlementId(), userId)
				.map(entitlementType -> new ActiveEntitlement(entitlementType, expiresAt));
	}

	private static Instant toInstant(Long epochMillis) {
		if (epochMillis == null) {
			return null;
		}
		return Instant.ofEpochMilli(epochMillis);
	}

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
			if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				return null;
			}
			throw new EntitlementProviderUnavailableException(e);
		} catch (RestClientException e) {
			throw new EntitlementProviderUnavailableException(e);
		}
	}
}
