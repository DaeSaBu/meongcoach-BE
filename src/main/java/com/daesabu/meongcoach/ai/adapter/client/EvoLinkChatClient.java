package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.client.dto.EvoLinkChatResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * EvoLink 채팅 API를 호출하는 공용 클라이언트. 영상 분석·제목 생성 어댑터가 같은 엔드포인트를 쓰므로
 * 전송과 응답 검증을 여기로 모은다. 전역 read-timeout(3초)으로는 모델 응답 전에 끊기므로
 * 이 클라이언트만 별도 requestFactory로 응답 타임아웃을 늘린다.
 * HTTP 오류(4xx·5xx)는 RestClient 예외를 그대로 전파하고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
class EvoLinkChatClient {

	private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final String FINISH_REASON_CONTENT_FILTER = "content_filter";
	private static final String FINISH_REASON_LENGTH = "length";

	private final EvoLinkProperties properties;
	private final RestClient restClient;

	// 생성자가 둘이라 주입 대상을 명시한다
	@Autowired
	EvoLinkChatClient(EvoLinkProperties properties, RestClient.Builder restClientBuilder) {
		this(properties, restClientBuilder
				.requestFactory(ClientHttpRequestFactoryBuilder.detect()
						.build(HttpClientSettings.defaults()
								.withConnectTimeout(CONNECT_TIMEOUT)
								.withReadTimeout(properties.responseTimeout())))
				.build());
	}

	// MockRestServiceServer로 응답을 가로챌 수 있도록 완성된 RestClient를 직접 받는 통로를 둔다.
	// baseUrl·인증 헤더를 빌더 기본값이 아니라 요청 코드에서 붙이는 것도 이 경로에서 같이 검증되게 하기 위해서다
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
			throw new IllegalStateException("EvoLink 응답에 choices가 없습니다");
		}
		EvoLinkChatResponse.Choice choice = response.choices().getFirst();
		if (FINISH_REASON_CONTENT_FILTER.equals(choice.finishReason())) {
			throw new IllegalStateException("EvoLink 응답이 콘텐츠 검토로 차단되었습니다");
		}
		if (FINISH_REASON_LENGTH.equals(choice.finishReason())) {
			throw new IllegalStateException("EvoLink 응답이 max_tokens 상한에 걸려 잘렸습니다");
		}
		if (choice.message() == null || choice.message().content() == null
				|| choice.message().content().isBlank()) {
			throw new IllegalStateException("EvoLink 응답 내용이 비어 있습니다");
		}
		return choice.message().content();
	}
}
