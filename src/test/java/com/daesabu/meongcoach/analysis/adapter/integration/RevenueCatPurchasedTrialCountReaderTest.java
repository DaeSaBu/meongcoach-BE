package com.daesabu.meongcoach.analysis.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.daesabu.meongcoach.analysis.domain.exception.AiTrialProviderUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RevenueCatPurchasedTrialCountReaderTest {

	private static final String BASE_URL = "https://api.revenuecat.test/v2";
	private static final String API_KEY = "test-revenuecat-api-key";
	private static final String PROJECT_ID = "proj_test";
	private static final String CURRENCY_CODE = "CREDIT";
	private static final Long USER_ID = 42L;
	private static final String VIRTUAL_CURRENCIES_URL =
			BASE_URL + "/projects/proj_test/customers/42/virtual_currencies";

	private MockRestServiceServer server;
	private RevenueCatPurchasedTrialCountReader reader;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		reader = new RevenueCatPurchasedTrialCountReader(
				new RevenueCatProperties(BASE_URL, API_KEY, PROJECT_ID, CURRENCY_CODE), builder);
	}

	private static String balance(String currencyCode, int balance) {
		return """
				{"object": "virtual_currency_balance", "currency_code": "%s", "balance": %d}
				""".formatted(currencyCode, balance);
	}

	private static String virtualCurrencies(String... items) {
		return """
				{"object": "list", "items": [%s], "next_page": null,
				 "url": "https://api.revenuecat.test/v2/projects/proj_test/customers/42/virtual_currencies?include_empty_balances=False"}
				""".formatted(String.join(",", items));
	}

	@Test
	void 고객의_잔액을_Bearer_인증으로_조회해_설정한_통화의_잔액을_반환한다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andRespond(withSuccess(virtualCurrencies(balance("CREDIT", 21)), MediaType.APPLICATION_JSON));

		int purchasedCount = reader.readPurchasedCount(USER_ID);

		assertThat(purchasedCount).isEqualTo(21);
		server.verify();
	}

	@Test
	void 다른_통화의_잔액은_세지_않는다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL))
				.andRespond(withSuccess(virtualCurrencies(balance("GEM", 500), balance("CREDIT", 3)),
						MediaType.APPLICATION_JSON));

		int purchasedCount = reader.readPurchasedCount(USER_ID);

		assertThat(purchasedCount).isEqualTo(3);
	}

	@Test
	void 설정한_통화의_잔액이_없으면_0을_반환한다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL))
				.andRespond(withSuccess(virtualCurrencies(), MediaType.APPLICATION_JSON));

		int purchasedCount = reader.readPurchasedCount(USER_ID);

		assertThat(purchasedCount).isZero();
	}

	@Test
	void RevenueCat에_고객이_없으면_0을_반환한다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL)).andRespond(withResourceNotFound());

		int purchasedCount = reader.readPurchasedCount(USER_ID);

		assertThat(purchasedCount).isZero();
	}

	@Test
	void API_키에_조회_권한이_없으면_구매_횟수_조회_불가_예외를_던진다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL)).andRespond(withForbiddenRequest());

		assertThatThrownBy(() -> reader.readPurchasedCount(USER_ID))
				.isInstanceOf(AiTrialProviderUnavailableException.class);
	}

	@Test
	void RevenueCat_호출_한도를_넘으면_구매_횟수_조회_불가_예외를_던진다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL)).andRespond(withTooManyRequests());

		assertThatThrownBy(() -> reader.readPurchasedCount(USER_ID))
				.isInstanceOf(AiTrialProviderUnavailableException.class);
	}

	@Test
	void RevenueCat_서버_오류면_구매_횟수_조회_불가_예외를_던진다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> reader.readPurchasedCount(USER_ID))
				.isInstanceOf(AiTrialProviderUnavailableException.class);
	}

	@Test
	void RevenueCat에_연결하지_못하면_구매_횟수_조회_불가_예외를_던진다() {
		server.expect(requestTo(VIRTUAL_CURRENCIES_URL))
				.andRespond(withException(new IOException("connection refused")));

		assertThatThrownBy(() -> reader.readPurchasedCount(USER_ID))
				.isInstanceOf(AiTrialProviderUnavailableException.class);
	}
}
