package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
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
import tools.jackson.databind.ObjectMapper;

class GeminiVideoAnalyzerTest {

	private static final String MODEL = "gemini-test";
	private static final String REGISTER_PATH = "/v1beta/files:register";
	private static final String GENERATE_PATH = "/v1beta/models/" + MODEL + ":generateContent";
	private static final String VIDEO_URI = "gs://test-ai-video-bucket/videos/training/7/key.mp4";
	private static final String REGISTERED_FILE_URI = "https://generativelanguage.googleapis.com/v1beta/files/abc";
	private static final List<TopicResult> TOPICS = List.of(
			new TopicResult(104L, "입질", "무는 습관 교정하기"),
			new TopicResult(106L, "분리불안", "혼자서도 편안하게"));
	private static final String VALID_CONTENT_JSON = "{\"recommend\":[{\"topicId\":104,\"title\":\"입질\","
			+ "\"description\":\"물건을 무는 습관을 줄이는 교육이라 도움이 돼요.\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"슬리퍼를 물고 달려요.\"}],"
			+ "\"solution\":[{\"order\":1,\"title\":\"교환 놀이 연습하기\",\"description\":\"간식과 바꿔 주세요.\"}]}";
	private static final String REPORT_TITLE = "슬리퍼를 물고 달리는 행동 분석";
	private static final String VALID_RESPONSE_JSON = VALID_CONTENT_JSON.substring(0, VALID_CONTENT_JSON.length() - 1)
			+ ",\"title\":\"" + REPORT_TITLE + "\"}";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<RecordedRequest> requests = new ArrayList<>();
	private MockWebServer server;
	private MockResponse registerResponse;
	private MockResponse generateResponse;
	private GeminiVideoAnalyzer analyzer;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				requests.add(request);
				if (Objects.requireNonNull(request.getPath()).startsWith(REGISTER_PATH)) {
					return registerResponse;
				}
				if (request.getPath().startsWith(GENERATE_PATH)) {
					return generateResponse;
				}
				return new MockResponse().setResponseCode(404);
			}
		});
		server.start();
		registerResponse = json(registerResponseJson("video/mp4"));

		TopicFinder topicFinder = mock(TopicFinder.class);
		when(topicFinder.findAllOrdered()).thenReturn(TOPICS);
		GeminiProperties properties = new GeminiProperties(
				"test-gemini-api-key", MODEL, Duration.ofMinutes(5), 4096, 0.0f, "low", 1.0);
		Client client = Client.builder()
				.apiKey(properties.apiKey())
				.httpOptions(HttpOptions.builder().baseUrl(server.url("/").toString()).build())
				.build();
		GoogleCredentials credentials = GoogleCredentials.create(new AccessToken("test-access-token", null));
		analyzer = new GeminiVideoAnalyzer(client, properties, credentials, topicFinder, objectMapper);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	private static MockResponse json(String body) {
		return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body);
	}

	private String registerResponseJson(String mimeType) {
		return objectMapper.writeValueAsString(Map.of("files", List.of(Map.of(
				"name", "files/abc",
				"uri", REGISTERED_FILE_URI,
				"mimeType", mimeType))));
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

	@Test
	void 모델이_반환한_JSON에서_제목을_분리하고_본문을_정규화해_반환한다() {
		givenModelResponds(VALID_RESPONSE_JSON);

		VideoAnalysisResult result = analyzer.analyze(VIDEO_URI);

		assertThat(result.title()).isEqualTo(REPORT_TITLE);
		assertThat(result.content()).isEqualTo(VALID_CONTENT_JSON);
	}

	@Test
	void 응답에_제목이_없어도_분석에_성공하고_제목은_null이다() {
		givenModelResponds(VALID_CONTENT_JSON);

		VideoAnalysisResult result = analyzer.analyze(VIDEO_URI);

		assertThat(result.title()).isNull();
		assertThat(result.content()).isEqualTo(VALID_CONTENT_JSON);
	}

	@Test
	void 응답_스키마의_마지막_필수_키로_제목을_요청한다() {
		givenModelResponds(VALID_RESPONSE_JSON);

		analyzer.analyze(VIDEO_URI);

		String body = requestTo(GENERATE_PATH).getBody().readUtf8();
		assertThat(objectMapper.readTree(body).at("/generationConfig/responseJsonSchema/required").toString())
				.isEqualTo("[\"recommend\",\"report\",\"solution\",\"title\"]");
	}

	@Test
	void 영상을_먼저_등록하고_등록된_파일_URI와_프레임_빈도로_분석을_요청한다() {
		givenModelResponds(VALID_RESPONSE_JSON);

		analyzer.analyze(VIDEO_URI);

		RecordedRequest register = requestTo(REGISTER_PATH);
		assertThat(register.getMethod()).isEqualTo("POST");
		assertThat(register.getHeader("Authorization")).isEqualTo("Bearer test-access-token");
		assertThat(register.getBody().readUtf8()).contains("\"uris\":[\"" + VIDEO_URI + "\"]");

		RecordedRequest generate = requestTo(GENERATE_PATH);
		assertThat(generate.getHeader("x-goog-api-key")).isEqualTo("test-gemini-api-key");
		String body = generate.getBody().readUtf8();
		assertThat(body)
				.contains("\"fileUri\":\"" + REGISTERED_FILE_URI + "\"")
				.contains("\"mimeType\":\"video/mp4\"")
				.contains("\"fps\":1.0")
				.contains("\"responseMimeType\":\"application/json\"")
				.contains("\"responseJsonSchema\"")
				.contains("\"thinkingLevel\":\"low\"")
				.contains("\"maxOutputTokens\":4096")
				.contains("\"systemInstruction\"");
	}

	@Test
	void 사용자_프롬프트에_교육_목록을_topicId와_함께_넣는다() {
		givenModelResponds(VALID_RESPONSE_JSON);

		analyzer.analyze(VIDEO_URI);

		String body = requestTo(GENERATE_PATH).getBody().readUtf8();
		assertThat(body)
				.contains("topicId: 104, 교육 이름: 입질, 설명: 무는 습관 교정하기")
				.contains("topicId: 106, 교육 이름: 분리불안, 설명: 혼자서도 편안하게")
				.doesNotContain("{{topics}}");
	}

	@Test
	void recommend와_solution이_없으면_빈_배열로_정규화한다() {
		givenModelResponds("{\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"산책 중이에요.\"}]}");

		String content = analyzer.analyze(VIDEO_URI).content();

		assertThat(content)
				.contains("\"recommend\":[]")
				.contains("\"solution\":[]");
	}

	@Test
	void 응답이_JSON_형식이_아니면_분석에_실패한다() {
		givenModelResponds("```json\n" + VALID_CONTENT_JSON + "\n```");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "{}", "{\"report\":null}", "{\"report\":[]}"})
	void 유효한_report가_없으면_분석에_실패한다(String response) {
		givenModelResponds(response);

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("report 항목이 없습니다");
	}

	@Test
	void 추천_교육의_topicId가_교육_목록에_없으면_분석에_실패한다() {
		givenModelResponds("{\"recommend\":[{\"topicId\":999,\"title\":\"없는 교육\",\"description\":\"설명\"}],"
				+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"달려요.\"}],\"solution\":[]}");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("999");
	}

	@Test
	void 응답_내용이_비어_있으면_분석에_실패한다() {
		givenModelResponds("");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("비어 있습니다");
	}

	@Test
	void 응답이_토큰_상한에_걸려_잘리면_분석에_실패한다() {
		generateResponse = json(generateResponseJson(VALID_CONTENT_JSON, "MAX_TOKENS"));

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("max_output_tokens");
	}

	@Test
	void 응답이_안전_검토로_중단되면_분석에_실패한다() {
		generateResponse = json(generateResponseJson("", "SAFETY"));

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("SAFETY");
	}

	@Test
	void 응답에_candidates가_없으면_분석에_실패한다() {
		generateResponse = json("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("candidates");
	}

	@Test
	void 모델_API가_오류를_응답하면_분석에_실패한다() {
		generateResponse = new MockResponse().setResponseCode(500)
				.setBody("{\"error\":{\"code\":500,\"message\":\"internal\",\"status\":\"INTERNAL\"}}");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining(VIDEO_URI);
	}

	@Test
	void 영상_등록_API가_오류를_응답하면_분석_호출_없이_실패한다() {
		registerResponse = new MockResponse().setResponseCode(403)
				.setBody("{\"error\":{\"code\":403,\"message\":\"forbidden\",\"status\":\"PERMISSION_DENIED\"}}");
		givenModelResponds(VALID_RESPONSE_JSON);

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("등록");
		assertThat(requests).noneMatch(request -> Objects.requireNonNull(request.getPath()).startsWith(GENERATE_PATH));
	}

	@Test
	void 영상_등록_응답에_파일이_없으면_분석에_실패한다() {
		registerResponse = json("{\"files\":[]}");
		givenModelResponds(VALID_RESPONSE_JSON);

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URI))
				.isInstanceOf(VideoAnalysisFailedException.class)
				.hasMessageContaining("파일 URI가 없습니다");
	}
}
