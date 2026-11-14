package com.daesabu.meongcoach.llm.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.llm.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.llm.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.llm.domain.exception.AiContentGenerationFailedException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.HttpOptions;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GeminiContentGeneratorTest {

	private static final String MODEL = "gemini-test";
	private static final String GENERATE_PATH = "/v1beta/models/" + MODEL + ":generateContent";
	private static final String SCHEMA = """
			{"type":"object","properties":{"goal":{"type":"string"}},
			"required":["goal"],"additionalProperties":false}
			""";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<RecordedRequest> requests = new ArrayList<>();
	private MockWebServer server;
	private MockResponse generateResponse;
	private AiContentGenerator generator;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				requests.add(request);
				if (Objects.requireNonNull(request.getPath()).startsWith(GENERATE_PATH)) {
					return generateResponse;
				}
				return new MockResponse().setResponseCode(404);
			}
		});
		server.start();

		GeminiProperties properties = new GeminiProperties(
				"test-gemini-api-key", MODEL, Duration.ofMinutes(5), 4096, 0.0f, "low", 1.0);
		Client client = Client.builder()
				.apiKey(properties.apiKey())
				.httpOptions(HttpOptions.builder().baseUrl(server.url("/").toString()).build())
				.build();
		generator = new GeminiContentGenerator(client, properties, objectMapper);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void 시스템_프롬프트와_스키마를_구조화_출력_설정으로_보내고_생성된_문자열을_그대로_반환한다() {
		String content = "{\"goal\":\"산책을 편안하게 해요\"}";
		givenModelResponds(content);

		String generated = generator.generate(request(SCHEMA));

		assertThat(generated).isEqualTo(content);
		RecordedRequest generate = requestTo(GENERATE_PATH);
		assertThat(generate.getHeader("x-goog-api-key")).isEqualTo("test-gemini-api-key");
		JsonNode body = objectMapper.readTree(generate.getBody().readUtf8());
		assertThat(body.at("/systemInstruction/parts/0/text").asString()).isEqualTo("시스템 지침");
		assertThat(body.at("/contents/0/parts/0/text").asString()).isEqualTo("사용자 입력");
		assertThat(body.at("/generationConfig/responseMimeType").asString()).isEqualTo("application/json");
		assertThat(body.at("/generationConfig/responseJsonSchema/properties/goal/type").asString())
				.isEqualTo("string");
		assertThat(body.at("/generationConfig/responseJsonSchema/additionalProperties").asBoolean()).isFalse();
		assertThat(body.at("/generationConfig/maxOutputTokens").asInt()).isEqualTo(4096);
		assertThat(body.at("/generationConfig/temperature").asDouble()).isEqualTo(0.0);
		assertThat(body.at("/generationConfig/thinkingConfig/thinkingLevel").asString()).isEqualTo("low");
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "[]", "\"문자열\"", "42", "true", ""})
	void 스키마가_JSON_객체가_아니면_외부_호출_전에_거절한다(String schema) {
		assertThatThrownBy(() -> generator.generate(request(schema)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("응답 스키마는 JSON 객체여야 합니다.");
		assertThat(requests).isEmpty();
	}

	@Test
	void 스키마_JSON_문법_오류는_외부_생성_실패로_변환하지_않는다() {
		assertThatThrownBy(() -> generator.generate(request("{잘못된 JSON")))
				.isInstanceOf(JacksonException.class);
		assertThat(requests).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 429, 500})
	void 모델_API_오류를_원인이_보존된_생성_실패로_변환한다(int status) {
		generateResponse = new MockResponse().setResponseCode(status)
				.setBody("{\"error\":{\"code\":" + status + ",\"message\":\"failed\",\"status\":\"ERROR\"}}");

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasCauseInstanceOf(ApiException.class);
	}

	@Test
	void 응답이_토큰_상한에_걸려_잘리면_생성에_실패한다() {
		generateResponse = json(generateResponseJson("{\"goal\":\"잘린", "MAX_TOKENS"));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasMessageContaining("max_output_tokens");
	}

	@Test
	void 응답이_안전_검토로_중단되면_생성에_실패한다() {
		generateResponse = json(generateResponseJson("", "SAFETY"));

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasMessageContaining("SAFETY");
	}

	@Test
	void 응답에_candidates가_없으면_생성에_실패한다() {
		generateResponse = json("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasMessageContaining("candidates");
	}

	@Test
	void 응답_내용이_비어_있으면_생성에_실패한다() {
		givenModelResponds(" ");

		assertThatThrownBy(() -> generator.generate(request(SCHEMA)))
				.isInstanceOf(AiContentGenerationFailedException.class)
				.hasMessageContaining("비어 있습니다");
	}

	private static AiContentGenerateRequest request(String schema) {
		return new AiContentGenerateRequest("시스템 지침", "사용자 입력", schema);
	}

	private static MockResponse json(String body) {
		return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body);
	}

	private String generateResponseJson(String text, String finishReason) {
		return objectMapper.writeValueAsString(Map.of("candidates", List.of(Map.of(
				"content", Map.of("role", "model", "parts", List.of(Map.of("text", text))),
				"finishReason", finishReason))));
	}

	private void givenModelResponds(String text) {
		generateResponse = json(generateResponseJson(text, "STOP"));
	}

	private RecordedRequest requestTo(String path) {
		return requests.stream()
				.filter(request -> Objects.requireNonNull(request.getPath()).startsWith(path))
				.findFirst()
				.orElseThrow();
	}
}
