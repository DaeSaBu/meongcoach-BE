package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.ai.domain.exception.AiContentGenerationFailedException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class EvoLinkContentGeneratorTest {

	private static final String BASE_URL = "https://api.evolink.test";
	private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";
	private static final String SCHEMA = """
			{"type":"object","properties":{"goal":{"type":"string"}},
			"required":["goal"],"additionalProperties":false}
			""";

	private MockRestServiceServer server;
	private AiContentGenerator generator;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		EvoLinkProperties properties = new EvoLinkProperties(
				BASE_URL, "test-key", Duration.ofMinutes(5), "test-model", 4096, 0.0, "disabled");
		EvoLinkChatClient client = new EvoLinkChatClient(properties, builder.build());
		generator = new EvoLinkContentGenerator(client, properties, new ObjectMapper());
	}

	@Test
	void 스키마를_JSON_객체로_전송하고_생성된_문자열을_그대로_반환한다() {
		String content = "{\"goal\":\"산책을 편안하게 해요\"}";
		server.expect(requestTo(CHAT_URL))
				.andExpect(jsonPath("$.model").value("test-model"))
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content").value("시스템 지침"))
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content").value("사용자 입력"))
				.andExpect(jsonPath("$.response_format.type").value("json_schema"))
				.andExpect(jsonPath("$.response_format.json_schema.name").value("generated_content"))
				.andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
				.andExpect(jsonPath("$.response_format.json_schema.schema").isMap())
				.andExpect(jsonPath("$.response_format.json_schema.schema.properties.goal.type").value("string"))
				.andExpect(jsonPath("$.max_tokens").value(4096))
				.andExpect(jsonPath("$.temperature").value(0.0))
				.andExpect(jsonPath("$.thinking.type").value("disabled"))
				.andRespond(withSuccess(response(content, "stop"), MediaType.APPLICATION_JSON));

		assertThat(generator.generate(request(SCHEMA))).isEqualTo(content);
		server.verify();
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "[]", "\"문자열\"", "42", "true", ""})
	void 스키마가_JSON_객체가_아니면_외부_호출_전에_거절한다(String schema) {
		assertThatThrownBy(() -> generator.generate(request(schema)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("응답 스키마는 JSON 객체여야 합니다.");
		server.verify();
	}

	@Test
	void 스키마_JSON_문법_오류는_외부_생성_실패로_변환하지_않는다() {
		assertThatThrownBy(() -> generator.generate(request("{잘못된 JSON")))
				.isInstanceOf(JacksonException.class);
		server.verify();
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 429, 500})
	void 외부_HTTP_오류를_원인이_보존된_생성_실패로_변환한다(int status) {
		server.expect(requestTo(CHAT_URL)).andRespond(withStatus(HttpStatusCode.valueOf(status)));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasCauseInstanceOf(RestClientResponseException.class);
		server.verify();
	}

	@Test
	void 타임아웃을_원인이_보존된_생성_실패로_변환한다() {
		server.expect(requestTo(CHAT_URL)).andRespond(withException(new SocketTimeoutException("timeout")));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasCauseInstanceOf(ResourceAccessException.class);
		server.verify();
	}

	@ParameterizedTest
	@ValueSource(strings = {"length", "content_filter"})
	void 잘리거나_차단된_응답을_생성_실패로_변환한다(String finishReason) {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(response("결과 일부", finishReason), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasCauseInstanceOf(EvoLinkResponseException.class);
		server.verify();
	}

	@Test
	void 빈_응답을_생성_실패로_변환한다() {
		server.expect(requestTo(CHAT_URL))
				.andRespond(withSuccess(response(" ", "stop"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasCauseInstanceOf(EvoLinkResponseException.class);
		server.verify();
	}

	private AiContentGenerateRequest request(String schema) {
		return new AiContentGenerateRequest("시스템 지침", "사용자 입력", schema);
	}

	private String response(String content, String finishReason) {
		Map<String, Object> response = Map.of("choices", List.of(Map.of(
				"message", Map.of("content", content), "finish_reason", finishReason)));
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(response);
	}
}
