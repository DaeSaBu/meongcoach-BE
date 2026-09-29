package com.daesabu.meongcoach.purchase.adapter.integration;

import com.daesabu.meongcoach.purchase.adapter.integration.dto.RevenueCatPurchasesResponse;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.StorePurchaseReader;
import com.daesabu.meongcoach.purchase.domain.exception.StorePurchaseUnavailableException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RevenueCat REST API v2로 회원의 일회성 구매 목록을 조회한다. 앱은 결제 전에 회원 ID로 RevenueCat에 로그인하므로
 * RevenueCat 고객 ID(app_user_id)가 곧 회원 ID다. 판매 상품이 평생 이용권뿐이라 회원당 구매가 적어 한 페이지(최대 100건)만 읽는다.
 */
@Slf4j
@Component
public class RevenueCatStorePurchaseReader implements StorePurchaseReader {

	private static final String PURCHASES_PATH = "/projects/{projectId}/customers/{customerId}/purchases?limit={limit}";
	private static final int PAGE_LIMIT = 100;

	private final RevenueCatProperties properties;
	private final RestClient restClient;

	public RevenueCatStorePurchaseReader(RevenueCatProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public List<PurchaseRegisterRequest> readOwnedPurchases(Long userId) {
		RevenueCatPurchasesResponse response = fetchPurchases(userId);
		if (response == null || response.items() == null) {
			return List.of();
		}
		if (response.hasNextPage()) {
			log.warn("RevenueCat 구매가 한 페이지({}건)를 넘어 나머지는 읽지 않았습니다: userId={}", PAGE_LIMIT, userId);
		}
		// 환불된 구매는 이용권을 주면 안 되므로 소유 중인 구매만 등록한다
		return response.items().stream()
				.filter(RevenueCatPurchasesResponse.Item::isOwned)
				.map(item -> item.toPurchaseRegisterRequest(userId))
				.toList();
	}

	private RevenueCatPurchasesResponse fetchPurchases(Long userId) {
		try {
			return restClient.get()
					.uri(properties.baseUrl() + PURCHASES_PATH, properties.projectId(), userId, PAGE_LIMIT)
					.headers(headers -> headers.setBearerAuth(properties.apiKey()))
					.retrieve()
					.body(RevenueCatPurchasesResponse.class);
		} catch (RestClientResponseException e) {
			// RevenueCat 고객은 SDK가 처음 로그인할 때 생긴다. 고객이 없으면 구매도 없는 것이다
			if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				return null;
			}
			throw new StorePurchaseUnavailableException(e);
		} catch (RestClientException e) {
			// 연결·타임아웃 실패다. 구매가 없는 것과 구분해야 클라이언트가 재시도할 수 있다
			throw new StorePurchaseUnavailableException(e);
		}
	}
}
