package com.daesabu.meongcoach.analysis.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatVirtualCurrenciesResponse(List<Item> items) {

	public RevenueCatVirtualCurrenciesResponse {
		items = Objects.requireNonNullElse(items, List.of());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Item(
			@JsonProperty("currency_code") String currencyCode,
			@JsonProperty("balance") int balance
	) {
	}
}
