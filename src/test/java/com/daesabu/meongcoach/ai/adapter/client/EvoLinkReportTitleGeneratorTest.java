package com.daesabu.meongcoach.ai.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
 * 모델 호출은 MockRestServiceServer로 가로채고, 제목 생성 요청 구성과 응답 정제를 검증한다.
 */
@DisplayName("EvoLink 리포트 제목 생성 어댑터")
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
		generator = new EvoLinkReportTitleGenerator(new EvoLinkChatClient(properties, builder.build()), properties);
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
	@DisplayName("모델이 반환한 제목을 그대로 반환한다")
	void generateTitleReturnsModelResponse() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("설정된 모델·최대 토큰·온도·사고 모드로 요청한다")
	void generateTitleUsesConfiguredModelSettings() {
		expectChatRequest()
				.andExpect(jsonPath("$.model").value(MODEL))
				.andExpect(jsonPath("$.max_tokens").value(4096))
				.andExpect(jsonPath("$.temperature").value(0.0))
				.andExpect(jsonPath("$.thinking.type").value("disabled"))
				.andRespond(withSuccess(chatResponseJson("물체를 물고 달리는 행동 분석"), MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	@DisplayName("제목은 평문이라 response_format을 지정하지 않는다")
	void generateTitleOmitsResponseFormat() {
		expectChatRequest()
				.andExpect(jsonPath("$.response_format").doesNotExist())
				.andRespond(withSuccess(chatResponseJson("물체를 물고 달리는 행동 분석"), MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	@DisplayName("영상 없이 텍스트 메시지만 보낸다")
	void generateTitleSendsTextOnlyMessage() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content", containsString("물체를 물고 달려요")))
				.andRespond(withSuccess(chatResponseJson("물체를 물고 달리는 행동 분석"), MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	@DisplayName("사용자 프롬프트에 리포트 JSON을 치환해 보낸다")
	void generateTitleInjectsReportIntoUserPrompt() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[1].content", not(containsString("{{report}}"))))
				.andRespond(withSuccess(chatResponseJson("물체를 물고 달리는 행동 분석"), MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	@DisplayName("제목 지시를 system 메시지로 보낸다")
	void generateTitleSendsInstructionAsSystemMessage() {
		expectChatRequest()
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content", containsString("제목")))
				.andRespond(withSuccess(chatResponseJson("물체를 물고 달리는 행동 분석"), MediaType.APPLICATION_JSON));

		generator.generateTitle(REPORT_JSON);

		server.verify();
	}

	@Test
	@DisplayName("코드 펜스와 여러 줄 응답에서 제목 한 줄만 추린다")
	void generateTitleKeepsFirstLineWithoutFences() {
		givenModelResponds("```\n물체를 물고 달리는 행동 분석\n부연 설명입니다.\n```");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("제목을 감싼 따옴표를 제거한다")
	void generateTitleStripsSurroundingQuotes() {
		givenModelResponds("\"물체를 물고 달리는 행동 분석\"");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("200자를 넘는 제목은 200자로 자른다")
	void generateTitleTruncatesTitleOverMaxLength() {
		givenModelResponds("가".repeat(250));

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).hasSize(200);
	}

	@Test
	@DisplayName("정제 후 제목이 비어 있으면 제목 생성에 실패한다")
	void generateTitleFailsWhenSanitizedTitleIsBlank() {
		givenModelResponds("```json\n```");

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(IllegalStateException.class);
	}
}
