package com.daesabu.meongcoach.analysis.adapter.integration;

import com.daesabu.meongcoach.analysis.adapter.integration.dto.RevenueCatVirtualCurrenciesResponse;
import com.daesabu.meongcoach.analysis.adapter.integration.dto.RevenueCatVirtualCurrenciesResponse.Item;
import com.daesabu.meongcoach.analysis.application.required.PurchasedTrialCountReader;
import com.daesabu.meongcoach.analysis.domain.exception.AiTrialProviderUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RevenueCatPurchasedTrialCountReader implements PurchasedTrialCountReader {

	private static final String VIRTUAL_CURRENCIES_PATH =
			"/projects/{projectId}/customers/{customerId}/virtual_currencies";

	private final RevenueCatProperties properties;
	private final RestClient restClient;

	public RevenueCatPurchasedTrialCountReader(RevenueCatProperties properties,
	                                           RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public int readPurchasedCount(Long userId) {
		RevenueCatVirtualCurrenciesResponse response = fetchVirtualCurrencies(userId);
		if (response == null) {
			return 0;
		}

		return response.items().stream()
				.filter(item -> properties.currencyCode().equals(item.currencyCode()))
				.mapToInt(Item::balance)
				.findFirst()
				.orElse(0);
	}

	private RevenueCatVirtualCurrenciesResponse fetchVirtualCurrencies(Long userId) {
		try {
			return restClient.get()
					.uri(properties.baseUrl() + VIRTUAL_CURRENCIES_PATH, properties.projectId(), userId)
					.headers(headers -> headers.setBearerAuth(properties.apiKey()))
					.retrieve()
					.body(RevenueCatVirtualCurrenciesResponse.class);
		} catch (RestClientResponseException e) {
			if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				return null;
			}
			throw new AiTrialProviderUnavailableException(e);
		} catch (RestClientException e) {
			throw new AiTrialProviderUnavailableException(e);
		}
	}
}
