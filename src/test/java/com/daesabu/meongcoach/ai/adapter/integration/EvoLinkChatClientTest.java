package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest;
import com.daesabu.meongcoach.ai.adapter.integration.dto.EvoLinkChatRequest.ChatMessage;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/**
 * HTTP 호출은 MockRestServiceServer로 가로채고, 요청 구성(엔드포인트·인증)과 응답 검증 규칙을 확인한다.
 */
@DisplayName("EvoLink 채팅 클라이언트")
class EvoLinkChatClientTest {

	private static final String BASE_URL = "https://api.evolink.test";
	private static final String API_KEY = "test-evolink-api-key";
	private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";

	private MockRestServiceServer server;
	private EvoLinkChatClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new EvoLinkChatClient(properties(), builder.build());
	}

	private static EvoLinkProperties properties() {
		return new EvoLinkProperties(BASE_URL, API_KEY, "doubao-seed-2.0-pro", Duration.ofMinutes(5),
				4096, 0.0, "disabled", 1.0);
	}

	private static EvoLinkChatRequest chatRequest() {
		return new EvoLinkChatRequest("doubao-seed-2.0-pro", List.of(ChatMessage.user("자기소개를 해주세요")),
				null, 4096, 0.0, null);
	}

	private static String chatResponseJson(String content, String finishReason) {
		return new ObjectMapper().writeValueAsString(Map.of(
				"id", "test-request-id",
				"choices", List.of(Map.of(
						"message", Map.of("role", "assistant", "content", content),
						"finish_reason", finishReason))));
	}

	@Test
	@DisplayName("chat/completions 엔드포인트에 Bearer 인증으로 POST 요청한다")
	void completePostsToChatCompletionsWithBearerAuth() {
		server.expect(requestTo(CHAT_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andRespond(withSuccess(chatResponseJson("안녕하세요", "stop"), MediaType.APPLICATION_JSON));

		String content = client.complete(chatRequest());

		assertThat(content).isEqualTo("안녕하세요");
		server.verify();
	}

	@Test
	@DisplayName("choices가 없는 응답이면 실패한다")
	void completeFailsWhenChoicesMissing() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess("{\"id\":\"test-request-id\",\"choices\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(EvoLinkResponseException.class);
	}

	@Test
	@DisplayName("콘텐츠 검토로 차단된 응답이면 실패한다")
	void completeFailsWhenContentFiltered() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(chatResponseJson("일부 내용", "content_filter"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(EvoLinkResponseException.class)
				.hasMessageContaining("콘텐츠 검토");
	}

	@Test
	@DisplayName("max_tokens 상한에 걸려 잘린 응답이면 실패한다")
	void completeFailsWhenTruncatedByMaxTokens() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(chatResponseJson("잘린 내용", "length"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(EvoLinkResponseException.class)
				.hasMessageContaining("max_tokens");
	}

	@Test
	@DisplayName("응답 내용이 비어 있으면 실패한다")
	void completeFailsWhenContentIsBlank() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(chatResponseJson(" ", "stop"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(EvoLinkResponseException.class);
	}

	@Test
	@DisplayName("4xx 응답은 RestClient 예외를 그대로 전파한다")
	void completePropagatesClientError() {
		// 재시도·폴백 없이 전파하고, 삼킬지는 호출부(AiReportGenerateService)가 정한다
		server.expect(requestTo(CHAT_URL))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(RestClientResponseException.class);
	}

	@Test
	@DisplayName("5xx 응답은 RestClient 예외를 그대로 전파한다")
	void completePropagatesServerError() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.complete(chatRequest()))
				.isInstanceOf(RestClientResponseException.class);
	}
}
