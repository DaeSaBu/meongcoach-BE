package com.daesabu.meongcoach.ai.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * 모델 호출은 MockRestServiceServer로 가로채고, 채팅 요청 구성과 응답 처리를 검증한다.
 */
@DisplayName("EvoLink 영상 분석 어댑터")
class EvoLinkVideoAnalyzerTest {

	private static final String BASE_URL = "https://api.evolink.test";
	private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";
	private static final String MODEL = "doubao-seed-2.0-pro";
	private static final String VIDEO_URL =
			"https://test-video-bucket.s3.amazonaws.com/videos/training/7/key.mp4?X-Amz-Signature=abc";
	// 정규화 재직렬화 결과와 비교할 수 있도록 record 컴포넌트 순서(recommend, report, solution)와 맞춘 JSON
	private static final String VALID_CONTENT_JSON = "{\"recommend\":[{\"title\":\"입질 교정\","
			+ "\"description\":\"물건을 무는 습관을 줄이는 교육이라 도움이 돼요.\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"슬리퍼를 물고 달려요.\"}],"
			+ "\"solution\":[{\"order\":1,\"title\":\"교환 놀이 연습하기\",\"description\":\"간식과 바꿔 주세요.\"}]}";

	private MockRestServiceServer server;
	private TopicFinder topicFinder;
	private EvoLinkVideoAnalyzer analyzer;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		topicFinder = mock(TopicFinder.class);
		EvoLinkProperties properties = new EvoLinkProperties(BASE_URL, "test-evolink-api-key", MODEL,
				Duration.ofMinutes(5), 4096, 0.0, "disabled", 1.0);
		analyzer = new EvoLinkVideoAnalyzer(new EvoLinkChatClient(properties, builder.build()),
				properties, topicFinder, new ObjectMapper());
	}

	private ResponseActions expectChatRequest() {
		return server.expect(requestTo(CHAT_URL)).andExpect(method(HttpMethod.POST));
	}

	private void givenModelResponds(String content) {
		expectChatRequest().andRespond(withSuccess(chatResponseJson(content), MediaType.APPLICATION_JSON));
	}

	private static String chatResponseJson(String content) {
		return new ObjectMapper().writeValueAsString(Map.of(
				"id", "test-request-id",
				"choices", List.of(Map.of(
						"message", Map.of("role", "assistant", "content", content),
						"finish_reason", "stop"))));
	}

	@Test
	@DisplayName("모델이 반환한 JSON을 정규화해 반환한다")
	void analyzeReturnsNormalizedJson() {
		givenModelResponds(VALID_CONTENT_JSON);

		String content = analyzer.analyze(VIDEO_URL);

		assertThat(content).isEqualTo(VALID_CONTENT_JSON);
	}

	@Test
	@DisplayName("json_schema strict를 어긴 코드 펜스 응답이면 분석에 실패한다")
	void analyzeFailsWhenResponseWrappedInFences() {
		givenModelResponds("```json\n" + VALID_CONTENT_JSON + "\n```");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URL))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("recommend와 solution이 없으면 빈 배열로 정규화한다")
	void analyzeNormalizesMissingListsToEmptyArrays() {
		givenModelResponds("{\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"산책 중이에요.\"}]}");

		String content = analyzer.analyze(VIDEO_URL);

		assertThat(content)
				.contains("\"recommend\":[]")
				.contains("\"solution\":[]");
	}

	@Test
	@DisplayName("응답에서 JSON 객체를 찾지 못하면 분석에 실패한다")
	void analyzeFailsWhenResponseHasNoJsonObject() {
		givenModelResponds("분리불안 징후가 관찰됩니다.");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URL))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("응답이 JSON 형식이 아니면 분석에 실패한다")
	void analyzeFailsWhenResponseIsMalformedJson() {
		givenModelResponds("{\"recommend\": [잘못된 형식}");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URL))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("report 항목이 비어 있으면 분석에 실패한다")
	void analyzeFailsWhenReportIsEmpty() {
		givenModelResponds("{\"recommend\":[],\"report\":[],\"solution\":[]}");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URL))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("실패 메시지에 presigned URL의 서명 쿼리를 남기지 않는다")
	void analyzeFailureMessageExcludesPresignedQuery() {
		givenModelResponds("분리불안 징후가 관찰됩니다.");

		assertThatThrownBy(() -> analyzer.analyze(VIDEO_URL))
				.hasMessageNotContaining("X-Amz-Signature");
	}

	@Test
	@DisplayName("설정된 모델·최대 토큰·온도·사고 모드로 요청한다")
	void analyzeUsesConfiguredModelSettings() {
		expectChatRequest()
				.andExpect(jsonPath("$.model").value(MODEL))
				.andExpect(jsonPath("$.max_tokens").value(4096))
				.andExpect(jsonPath("$.temperature").value(0.0))
				.andExpect(jsonPath("$.thinking.type").value("disabled"))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("리포트 구조의 json_schema를 strict로 지정한다")
	void analyzeRequestsStrictJsonSchemaResponseFormat() {
		expectChatRequest()
				.andExpect(jsonPath("$.response_format.type").value("json_schema"))
				.andExpect(jsonPath("$.response_format.json_schema.name").value("ai_report"))
				.andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
				.andExpect(jsonPath("$.response_format.json_schema.schema.type").value("object"))
				.andExpect(jsonPath("$.response_format.json_schema.schema.required",
						contains("recommend", "report", "solution")))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("설정된 API 키를 Bearer 토큰으로 보낸다")
	void analyzeSendsBearerAuthorization() {
		expectChatRequest()
				.andExpect(header("Authorization", "Bearer test-evolink-api-key"))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("비디오 블록을 텍스트보다 먼저 보낸다")
	void analyzeSendsVideoBeforeText() {
		// 순서가 뒤집히면 모델이 지시를 무시하고 영어 장면 묘사로 빠진다
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].content[0].type").value("video_url"))
				.andExpect(jsonPath("$.messages[1].content[1].type").value("text"))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("presigned URL을 video_url로 프레임 추출 빈도와 함께 실어 보낸다")
	void analyzeAttachesPresignedUrlAsVideoUrl() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].content[0].video_url.url").value(VIDEO_URL))
				.andExpect(jsonPath("$.messages[1].content[0].video_url.fps").value(1.0))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("분석 지시를 system 메시지로 보낸다")
	void analyzeSendsAnalysisInstructionAsSystemMessage() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content", containsString("반려견")))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}

	@Test
	@DisplayName("사용자 프롬프트에 교육 목록을 치환해 보낸다")
	void analyzeInjectsTopicsIntoUserPrompt() {
		when(topicFinder.findAllOrdered()).thenReturn(List.of(
				new TopicSummary(1L, "배변", "편안한 배변 습관 만들기"),
				new TopicSummary(2L, "분리불안", "혼자서도 편안하게")));
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].content[1].text", containsString("배변: 편안한 배변 습관 만들기")))
				.andExpect(jsonPath("$.messages[1].content[1].text", containsString("분리불안: 혼자서도 편안하게")))
				.andExpect(jsonPath("$.messages[1].content[1].text", not(containsString("{{topics}}"))))
				.andRespond(withSuccess(chatResponseJson(VALID_CONTENT_JSON), MediaType.APPLICATION_JSON));

		analyzer.analyze(VIDEO_URL);

		server.verify();
	}
}
