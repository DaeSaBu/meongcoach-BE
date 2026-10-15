package com.daesabu.meongcoach.entitlement.adapter.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RevenueCatEntitlementGranterTest {

	private static final String BASE_URL = "https://api.revenuecat.test/v2";
	private static final String API_KEY = "test-revenuecat-api-key";
	private static final String PROJECT_ID = "proj_test";
	private static final Long USER_ID = 42L;
	private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T05:03:00Z");
	private static final String GRANT_URL = BASE_URL + "/projects/proj_test/customers/42/actions/grant_entitlement";
	private static final String CUSTOMERS_URL = BASE_URL + "/projects/proj_test/customers";
	private static final Map<EntitlementType, String> ENTITLEMENT_IDS = Map.of(
			EntitlementType.PUPPY, "entl_puppy",
			EntitlementType.JUNIOR, "entl_junior",
			EntitlementType.ADULT, "entl_adult",
			EntitlementType.SENIOR, "entl_senior"
	);

	private MockRestServiceServer server;
	private RevenueCatEntitlementGranter granter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		granter = new RevenueCatEntitlementGranter(
				new RevenueCatProperties(BASE_URL, API_KEY, PROJECT_ID, ENTITLEMENT_IDS), builder);
	}

	@Test
	void 이용권_종류에_맞는_entitlement_id와_만료_시각으로_프로모션_이용권을_지급한다() {
		server.expect(once(), requestTo(GRANT_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.entitlement_id").value("entl_puppy"))
				.andExpect(jsonPath("$.expires_at").value(EXPIRES_AT.toEpochMilli()))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

		granter.grant(USER_ID, EntitlementType.PUPPY, EXPIRES_AT);

		server.verify();
	}

	@Test
	void 결제_대행에_없는_고객이면_고객을_만든_뒤_다시_지급한다() {
		server.expect(once(), requestTo(GRANT_URL))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withResourceNotFound());
		server.expect(once(), requestTo(CUSTOMERS_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.id").value("42"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));
		server.expect(once(), requestTo(GRANT_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.entitlement_id").value("entl_adult"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

		assertThatCode(() -> granter.grant(USER_ID, EntitlementType.ADULT, EXPIRES_AT)).doesNotThrowAnyException();

		server.verify();
	}

	@Test
	void 기존_지급과_2시간_이내로_겹쳐_중복으로_거절되면_이미_지급된_것으로_보고_넘어간다() {
		server.expect(once(), requestTo(GRANT_URL))
				.andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{}"));

		assertThatCode(() -> granter.grant(USER_ID, EntitlementType.PUPPY, EXPIRES_AT)).doesNotThrowAnyException();

		server.verify();
	}

	@Test
	void 결제_대행이_오류를_돌려주면_지급_불가_예외를_던진다() {
		server.expect(once(), requestTo(GRANT_URL))
				.andRespond(withServerError());

		assertThatThrownBy(() -> granter.grant(USER_ID, EntitlementType.PUPPY, EXPIRES_AT))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
	}

	@Test
	void 고객을_만든_뒤에도_404가_나면_지급_불가_예외를_던진다() {
		server.expect(once(), requestTo(GRANT_URL)).andRespond(withResourceNotFound());
		server.expect(once(), requestTo(CUSTOMERS_URL))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));
		server.expect(once(), requestTo(GRANT_URL)).andRespond(withResourceNotFound());

		assertThatThrownBy(() -> granter.grant(USER_ID, EntitlementType.PUPPY, EXPIRES_AT))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
	}
}
