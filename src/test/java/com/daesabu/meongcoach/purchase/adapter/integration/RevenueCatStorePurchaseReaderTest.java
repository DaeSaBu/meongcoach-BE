package com.daesabu.meongcoach.purchase.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.domain.Store;
import com.daesabu.meongcoach.purchase.domain.exception.StorePurchaseUnavailableException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * HTTP 호출은 MockRestServiceServer로 가로채고, 요청 구성(엔드포인트·인증)과 응답을 구매 등록 입력으로 바꾸는 규칙을 확인한다.
 */
class RevenueCatStorePurchaseReaderTest {

	private static final String BASE_URL = "https://api.revenuecat.test/v2";
	private static final String API_KEY = "test-revenuecat-api-key";
	private static final String PROJECT_ID = "proj_test";
	private static final Long USER_ID = 42L;
	private static final String PURCHASES_URL = BASE_URL + "/projects/proj_test/customers/42/purchases?limit=100";
	private static final long PURCHASED_AT_MS = 1790181485867L;

	private MockRestServiceServer server;
	private RevenueCatStorePurchaseReader reader;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		reader = new RevenueCatStorePurchaseReader(new RevenueCatProperties(BASE_URL, API_KEY, PROJECT_ID), builder);
	}

	private static String purchase(String storePurchaseIdentifier, String status, String... lookupKeys) {
		String entitlements = String.join(",", Arrays.stream(lookupKeys)
				.map(lookupKey -> """
						{"object": "entitlement", "id": "entl_%s", "lookup_key": "%s", "display_name": "%s"}
						""".formatted(lookupKey, lookupKey, lookupKey))
				.toList());
		return """
				{
				  "object": "purchase",
				  "id": "purch_%s",
				  "customer_id": "42",
				  "original_customer_id": "42",
				  "product_id": "prod1a2b3c4d5e",
				  "purchased_at": %d,
				  "revenue_in_usd": {"currency": "USD", "gross": 6.99, "commission": 2.1, "tax": 0.64, "proceeds": 4.25},
				  "quantity": 1,
				  "status": "%s",
				  "presented_offering_id": "default",
				  "entitlements": {"object": "list", "items": [%s], "next_page": null, "url": "/v2/entitlements"},
				  "environment": "sandbox",
				  "store": "app_store",
				  "store_purchase_identifier": "%s",
				  "ownership": "purchased"
				}
				""".formatted(storePurchaseIdentifier, PURCHASED_AT_MS, status, entitlements, storePurchaseIdentifier);
	}

	private static String purchases(String... items) {
		return """
				{"object": "list", "items": [%s], "next_page": null, "url": "/v2/projects/proj_test/customers/42/purchases"}
				""".formatted(String.join(",", items));
	}

	@Test
	void 고객_구매_목록을_Bearer_인증으로_조회해_소유한_구매를_등록_입력으로_바꾼다() {
		server.expect(requestTo(PURCHASES_URL))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andRespond(withSuccess(purchases(purchase("2000000912345678", "owned", "puppy", "junior")),
						MediaType.APPLICATION_JSON));

		List<PurchaseRegisterRequest> requests = reader.readOwnedPurchases(USER_ID);

		assertThat(requests).containsExactly(new PurchaseRegisterRequest(
				USER_ID, "2000000912345678", Store.APP_STORE, new BigDecimal("6.99"), "USD",
				Instant.ofEpochMilli(PURCHASED_AT_MS), Set.of(EntitlementType.PUPPY, EntitlementType.JUNIOR)));
		server.verify();
	}

	@Test
	void 환불된_구매는_제외한다() {
		server.expect(requestTo(PURCHASES_URL))
				.andRespond(withSuccess(purchases(
						purchase("2000000900000001", "refunded", "puppy"),
						purchase("2000000900000002", "owned", "adult")
				), MediaType.APPLICATION_JSON));

		List<PurchaseRegisterRequest> requests = reader.readOwnedPurchases(USER_ID);

		assertThat(requests).extracting(PurchaseRegisterRequest::transactionId).containsExactly("2000000900000002");
	}

	@Test
	void RevenueCat에_고객이_없으면_빈_목록을_반환한다() {
		server.expect(requestTo(PURCHASES_URL)).andRespond(withResourceNotFound());

		List<PurchaseRegisterRequest> requests = reader.readOwnedPurchases(USER_ID);

		assertThat(requests).isEmpty();
	}

	@Test
	void RevenueCat_서버_오류면_구매_조회_불가_예외를_던진다() {
		server.expect(requestTo(PURCHASES_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> reader.readOwnedPurchases(USER_ID))
				.isInstanceOf(StorePurchaseUnavailableException.class);
	}

	@Test
	void RevenueCat에_연결하지_못하면_구매_조회_불가_예외를_던진다() {
		server.expect(requestTo(PURCHASES_URL)).andRespond(withException(new IOException("connection refused")));

		assertThatThrownBy(() -> reader.readOwnedPurchases(USER_ID))
				.isInstanceOf(StorePurchaseUnavailableException.class);
	}
}
