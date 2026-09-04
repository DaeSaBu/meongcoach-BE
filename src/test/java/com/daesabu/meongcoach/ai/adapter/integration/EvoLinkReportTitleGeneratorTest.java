package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException;
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

/**
 * 모델 호출은 MockRestServiceServer로 가로채고, 제목 생성 요청 구성과 {"title": ...} 응답 파싱을 검증한다.
 */
class EvoLinkReportTitleGeneratorTest {

	private static final String BASE_URL = "https://api.evolink.test";
	private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";
	private static final String MODEL = "doubao-seed-2.0-pro";
	private static final String REPORT_JSON = "{\"recommend\":[],\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\","
			+ "\"description\":\"물체를 물고 달려요.\"}],\"solution\":[]}";

	private MockRestServiceServer server;
	private EvoLinkReportTitleGenerator generator;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		EvoLinkProperties properties = new EvoLinkProperties(BASE_URL, "test-evolink-api-key", MODEL,
				Duration.ofMinutes(5), 4096, 0.0, "disabled", 1.0);
		generator = new EvoLinkReportTitleGenerator(new EvoLinkChatClient(properties, builder.build()),
				properties, new ObjectMapper());
	}

	private ResponseActions expectChatRequest() {
		return server.expect(requestTo(CHAT_URL)).andExpect(method(HttpMethod.POST));
	}

	private void givenModelRespondsTitle(String title) {
		givenModelRespondsRaw(new ObjectMapper().writeValueAsString(Map.of("title", title)));
	}

	private void givenModelRespondsRaw(String content) {
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
	void 응답_JSON의_title을_제목으로_반환한다() {
		givenModelRespondsTitle("물체를 물고 달리는 행동 분석");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	void 설정된_모델_최대_토큰_온도_사고_모드로_요청한다() {
		expectChatRequest()
				.andExpect(jsonPath("$.model").value(MODEL))
				.andExpect(jsonPath("$.max_tokens").value(4096))
				.andExpect(jsonPath("$.temperature").value(0.0))
				.andExpect(jsonPath("$.thinking.type").value("disabled"))
				.andRespond(withSuccess(chatResponseJson("{\"title\":\"물체를 물고 달리는 행동 분석\"}"),
						MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	void 제목_구조의_json_schema를_strict로_지정한다() {
		expectChatRequest()
				.andExpect(jsonPath("$.response_format.type").value("json_schema"))
				.andExpect(jsonPath("$.response_format.json_schema.name").value("report_title"))
				.andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
				.andExpect(jsonPath("$.response_format.json_schema.schema.properties.title.type").value("string"))
				.andRespond(withSuccess(chatResponseJson("{\"title\":\"물체를 물고 달리는 행동 분석\"}"),
						MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	void 영상_없이_텍스트_메시지만_보낸다() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content", containsString("물체를 물고 달려요")))
				.andRespond(withSuccess(chatResponseJson("{\"title\":\"물체를 물고 달리는 행동 분석\"}"),
						MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	void 사용자_프롬프트에_리포트_JSON을_치환해_보낸다() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].content", not(containsString("{{report}}"))))
				.andRespond(withSuccess(chatResponseJson("{\"title\":\"물체를 물고 달리는 행동 분석\"}"),
						MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	void 제목_지시를_system_메시지로_보낸다() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content", containsString("제목")))
				.andRespond(withSuccess(chatResponseJson("{\"title\":\"물체를 물고 달리는 행동 분석\"}"),
						MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	void 제목_앞뒤_공백은_제거한다() {
		givenModelRespondsTitle("  물체를 물고 달리는 행동 분석  ");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	void json_schema_strict를_어긴_평문_응답이면_제목_생성에_실패한다() {
		givenModelRespondsRaw("물체를 물고 달리는 행동 분석");

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(ReportTitleGenerationFailedException.class);
	}

	@Test
	void 응답에_title_항목이_없으면_제목_생성에_실패한다() {
		givenModelRespondsRaw("{\"summary\":\"제목이 아닌 필드\"}");

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(ReportTitleGenerationFailedException.class);
	}

	@Test
	void 모델_API가_오류를_응답하면_ReportTitleGenerationFailedException으로_실패한다() {
		server.expect(requestTo(CHAT_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(ReportTitleGenerationFailedException.class);
	}

	@Test
	void 제목이_비어_있으면_제목_생성에_실패한다() {
		givenModelRespondsTitle(" ");

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(ReportTitleGenerationFailedException.class);
	}
}
