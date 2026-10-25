package com.daesabu.meongcoach.ai.adapter.integration;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class EvoLinkChatClient {

	private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final String FINISH_REASON_CONTENT_FILTER = "content_filter";
	private static final String FINISH_REASON_LENGTH = "length";

	private final EvoLinkProperties properties;
	private final RestClient restClient;

	@Autowired
	EvoLinkChatClient(EvoLinkProperties properties, RestClient.Builder restClientBuilder) {
		this(properties, restClientBuilder
				.requestFactory(ClientHttpRequestFactoryBuilder.detect()
						.build(HttpClientSettings.defaults()
								.withConnectTimeout(CONNECT_TIMEOUT)
								.withReadTimeout(properties.responseTimeout())))
				.build());
	}

	EvoLinkChatClient(EvoLinkProperties properties, RestClient restClient) {
		this.properties = properties;
		this.restClient = restClient;
	}

	String complete(EvoLinkChatRequest request) {
		EvoLinkChatResponse response = restClient.post()
				.uri(properties.baseUrl() + CHAT_COMPLETIONS_PATH)
				.headers(headers -> headers.setBearerAuth(properties.apiKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(EvoLinkChatResponse.class);
		return extractContent(response);
	}

	private static String extractContent(EvoLinkChatResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			throw new EvoLinkResponseException("EvoLink 응답에 choices가 없습니다");
		}
		EvoLinkChatResponse.Choice choice = response.choices().getFirst();
		if (FINISH_REASON_CONTENT_FILTER.equals(choice.finishReason())) {
			throw new EvoLinkResponseException("EvoLink 응답이 콘텐츠 검토로 차단되었습니다");
		}
		if (FINISH_REASON_LENGTH.equals(choice.finishReason())) {
			throw new EvoLinkResponseException("EvoLink 응답이 max_tokens 상한에 걸려 잘렸습니다");
		}
		if (choice.message() == null || choice.message().content() == null
				|| choice.message().content().isBlank()) {
			throw new EvoLinkResponseException("EvoLink 응답 내용이 비어 있습니다");
		}
		return choice.message().content();
	}
}
