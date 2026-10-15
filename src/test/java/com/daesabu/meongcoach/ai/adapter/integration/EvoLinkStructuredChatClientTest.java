package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.domain.shared.StructuredChatCompletionFailedException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class EvoLinkStructuredChatClientTest {

	private static final String BASE_URL = "https://api.evolink.test";
	private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";
	private static final String MODEL = "doubao-seed-2.0-pro";
	private static final String SCHEMA_JSON = "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}}}";
	private static final StructuredChatRequest REQUEST = new StructuredChatRequest("시스템 지시", "사용자 입력", "plan_draft",
			SCHEMA_JSON);

	private MockRestServiceServer server;
	private EvoLinkStructuredChatClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		EvoLinkProperties properties = new EvoLinkProperties(BASE_URL, "test-evolink-api-key", MODEL,
				Duration.ofMinutes(5), 4096, 0.0, "disabled", 1.0);
		client = new EvoLinkStructuredChatClient(new EvoLinkChatClient(properties, builder.build()), properties,
				new ObjectMapper());
	}

	private ResponseActions expectChatRequest() {
		return server.expect(requestTo(CHAT_URL)).andExpect(method(HttpMethod.POST));
	}

	private static String chatResponseJson(String content, String finishReason) {
		return new ObjectMapper().writeValueAsString(Map.of(
				"id", "test-request-id",
				"choices", List.of(Map.of(
						"message", Map.of("role", "assistant", "content", content),
						"finish_reason", finishReason))));
	}

	@Test
	void 모델_응답_content를_그대로_반환한다() {
		expectChatRequest().andRespond(withSuccess(chatResponseJson("{\"title\":\"계획\"}", "stop"),
				MediaType.APPLICATION_JSON));

		String content = client.complete(REQUEST);

		assertThat(content).isEqualTo("{\"title\":\"계획\"}");
	}

	@Test
	void system과_user_메시지를_순서대로_보낸다() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content").value("시스템 지시"))
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content").value("사용자 입력"))
				.andRespond(withSuccess(chatResponseJson("{}", "stop"), MediaType.APPLICATION_JSON));

		client.complete(REQUEST);

		server.verify();
	}

	@Test
	void 스키마를_json_schema_strict로_지정한다() {
		expectChatRequest()
				.andExpect(jsonPath("$.response_format.type").value("json_schema"))
				.andExpect(jsonPath("$.response_format.json_schema.name").value("plan_draft"))
				.andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
				.andExpect(jsonPath("$.response_format.json_schema.schema.properties.title.type").value("string"))
				.andRespond(withSuccess(chatResponseJson("{}", "stop"), MediaType.APPLICATION_JSON));

		client.complete(REQUEST);

		server.verify();
	}

	@Test
	void 설정된_모델_최대_토큰_온도_사고_모드로_요청한다() {
		expectChatRequest()
				.andExpect(jsonPath("$.model").value(MODEL))
				.andExpect(jsonPath("$.max_tokens").value(4096))
				.andExpect(jsonPath("$.temperature").value(0.0))
				.andExpect(jsonPath("$.thinking.type").value("disabled"))
				.andRespond(withSuccess(chatResponseJson("{}", "stop"), MediaType.APPLICATION_JSON));

		client.complete(REQUEST);

		server.verify();
	}

	@Test
	void 모델_API가_오류를_응답하면_StructuredChatCompletionFailedException으로_실패한다() {
		server.expect(requestTo(CHAT_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.complete(REQUEST))
				.isInstanceOf(StructuredChatCompletionFailedException.class);
	}

	@Test
	void 콘텐츠_검토로_차단된_응답이면_실패한다() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(chatResponseJson("일부 내용", "content_filter"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete(REQUEST))
				.isInstanceOf(StructuredChatCompletionFailedException.class);
	}

	@Test
	void 스키마가_JSON이_아니면_모델을_호출하지_않고_실패한다() {
		StructuredChatRequest request = new StructuredChatRequest("시스템", "사용자", "plan_draft", "{깨진 스키마");

		assertThatThrownBy(() -> client.complete(request))
				.isInstanceOf(StructuredChatCompletionFailedException.class);
		server.verify();
	}

	@Test
	void 요청의_타임아웃을_채팅_클라이언트에_전달한다() {
		EvoLinkChatClient chatClient = mock(EvoLinkChatClient.class);
		EvoLinkProperties properties = new EvoLinkProperties(BASE_URL, "test-evolink-api-key", MODEL,
				Duration.ofMinutes(5), 4096, 0.0, "disabled", 1.0);
		EvoLinkStructuredChatClient timeoutClient = new EvoLinkStructuredChatClient(chatClient, properties,
				new ObjectMapper());
		given(chatClient.complete(any(), any())).willReturn("{}");
		StructuredChatRequest request = new StructuredChatRequest("시스템 지시", "사용자 입력", "plan_draft", SCHEMA_JSON,
				Duration.ofSeconds(30));

		timeoutClient.complete(request);

		then(chatClient).should().complete(any(), eq(Duration.ofSeconds(30)));
	}

	@Test
	void 타임아웃이_없는_요청은_null로_전달해_기본_설정을_따른다() {
		EvoLinkChatClient chatClient = mock(EvoLinkChatClient.class);
		EvoLinkProperties properties = new EvoLinkProperties(BASE_URL, "test-evolink-api-key", MODEL,
				Duration.ofMinutes(5), 4096, 0.0, "disabled", 1.0);
		EvoLinkStructuredChatClient defaultClient = new EvoLinkStructuredChatClient(chatClient, properties,
				new ObjectMapper());
		given(chatClient.complete(any(), any())).willReturn("{}");

		defaultClient.complete(REQUEST);

		then(chatClient).should().complete(any(), isNull());
	}
}
