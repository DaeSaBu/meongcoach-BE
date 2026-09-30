package com.daesabu.meongcoach.entitlement.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RevenueCatActiveEntitlementReaderTest {

	private static final String BASE_URL = "https://api.revenuecat.test/v2";
	private static final String API_KEY = "test-revenuecat-api-key";
	private static final String PROJECT_ID = "proj_test";
	private static final Long USER_ID = 42L;
	private static final String ACTIVE_ENTITLEMENTS_URL =
			BASE_URL + "/projects/proj_test/customers/42/active_entitlements?limit=100";
	private static final Map<EntitlementType, String> ENTITLEMENT_IDS = Map.of(
			EntitlementType.PUPPY, "entl_puppy",
			EntitlementType.JUNIOR, "entl_junior",
			EntitlementType.ADULT, "entl_adult",
			EntitlementType.SENIOR, "entl_senior"
	);

	private MockRestServiceServer server;
	private RevenueCatActiveEntitlementReader reader;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		reader = new RevenueCatActiveEntitlementReader(
				new RevenueCatProperties(BASE_URL, API_KEY, PROJECT_ID, ENTITLEMENT_IDS), builder);
	}

	private static String activeEntitlements(String... entitlementIds) {
		String items = String.join(",", Arrays.stream(entitlementIds)
				.map(entitlementId -> """
						{"object": "customer.active_entitlement", "entitlement_id": "%s", "expires_at": null}
						""".formatted(entitlementId))
				.toList());
		return """
				{"object": "list", "items": [%s], "next_page": null,
				 "url": "/v2/projects/proj_test/customers/42/active_entitlements"}
				""".formatted(items);
	}

	@Test
	void 고객의_활성_이용권을_Bearer_인증으로_조회해_이용권_종류로_바꾼다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andRespond(withSuccess(activeEntitlements("entl_puppy", "entl_junior"), MediaType.APPLICATION_JSON));

		Set<EntitlementType> activeTypes = reader.readActiveTypes(USER_ID);

		assertThat(activeTypes).containsExactlyInAnyOrder(EntitlementType.PUPPY, EntitlementType.JUNIOR);
		server.verify();
	}

	@Test
	void 설정에_없는_이용권_ID는_무시하고_나머지를_반환한다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL))
				.andRespond(withSuccess(activeEntitlements("entl_unknown", "entl_adult"), MediaType.APPLICATION_JSON));

		Set<EntitlementType> activeTypes = reader.readActiveTypes(USER_ID);

		assertThat(activeTypes).containsExactly(EntitlementType.ADULT);
	}

	@Test
	void 활성_이용권이_없으면_빈_집합을_반환한다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL))
				.andRespond(withSuccess(activeEntitlements(), MediaType.APPLICATION_JSON));

		Set<EntitlementType> activeTypes = reader.readActiveTypes(USER_ID);

		assertThat(activeTypes).isEmpty();
	}

	@Test
	void RevenueCat에_고객이_없으면_빈_집합을_반환한다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL)).andRespond(withResourceNotFound());

		Set<EntitlementType> activeTypes = reader.readActiveTypes(USER_ID);

		assertThat(activeTypes).isEmpty();
	}

	@Test
	void RevenueCat_호출_한도를_넘으면_이용권_조회_불가_예외를_던진다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL)).andRespond(withTooManyRequests());

		assertThatThrownBy(() -> reader.readActiveTypes(USER_ID))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
	}

	@Test
	void RevenueCat_서버_오류면_이용권_조회_불가_예외를_던진다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> reader.readActiveTypes(USER_ID))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
	}

	@Test
	void RevenueCat에_연결하지_못하면_이용권_조회_불가_예외를_던진다() {
		server.expect(requestTo(ACTIVE_ENTITLEMENTS_URL))
				.andRespond(withException(new IOException("connection refused")));

		assertThatThrownBy(() -> reader.readActiveTypes(USER_ID))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
	}
}
