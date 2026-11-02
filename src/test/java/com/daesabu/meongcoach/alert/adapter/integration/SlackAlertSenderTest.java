package com.daesabu.meongcoach.alert.adapter.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.exception.AlertDeliveryFailedException;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackAlertSenderTest {

	private static final String WEBHOOK_URL = "https://hooks.slack.test/services/T000/B000/secret-token";
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-02T12:34:56Z");
	private static final List<AlertField> FIELDS = List.of(
			new AlertField("userId", "42"),
			new AlertField("reason", "Gemini 응답 시간 초과"));

	private MockRestServiceServer server;
	private RestClient.Builder builder;

	@BeforeEach
	void setUp() {
		builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
	}

	private SlackAlertSender sender(MockEnvironment environment) {
		return new SlackAlertSender(new SlackAlertProperties(true, WEBHOOK_URL), builder, environment);
	}

	private SlackAlertSender sender() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		return sender(environment);
	}

	private static Alert alert(String title, List<AlertField> fields) {
		return new Alert(title, fields, OCCURRED_AT);
	}

	@Test
	void webhook으로_header_section_context_블록을_담은_JSON을_POST한다() {
		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.text").value("영상 분석 실패"))
				.andExpect(jsonPath("$.blocks.length()").value(3))
				.andExpect(jsonPath("$.blocks[0].type").value("header"))
				.andExpect(jsonPath("$.blocks[0].text.type").value("plain_text"))
				.andExpect(jsonPath("$.blocks[0].text.text").value("영상 분석 실패"))
				.andExpect(jsonPath("$.blocks[1].type").value("section"))
				.andExpect(jsonPath("$.blocks[1].fields[0].type").value("mrkdwn"))
				.andExpect(jsonPath("$.blocks[1].fields[0].text").value("*userId*\n42"))
				.andExpect(jsonPath("$.blocks[1].fields[1].text").value("*reason*\nGemini 응답 시간 초과"))
				.andExpect(jsonPath("$.blocks[2].type").value("context"))
				.andExpect(jsonPath("$.blocks[2].elements[0].type").value("mrkdwn"))
				.andExpect(jsonPath("$.blocks[2].elements[0].text").value("test · 2026-11-02 21:34:56 KST"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		sender().send(alert("영상 분석 실패", FIELDS));

		server.verify();
	}

	@Test
	void 필드가_없으면_section_블록을_만들지_않는다() {
		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(jsonPath("$.blocks.length()").value(2))
				.andExpect(jsonPath("$.blocks[0].type").value("header"))
				.andExpect(jsonPath("$.blocks[1].type").value("context"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		sender().send(alert("신규 가입", List.of()));

		server.verify();
	}

	@Test
	void 필드가_10개를_넘으면_section_블록을_나눈다() {
		List<AlertField> fields = IntStream.range(0, 11)
				.mapToObj(index -> new AlertField("field" + index, String.valueOf(index)))
				.toList();
		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(jsonPath("$.blocks.length()").value(4))
				.andExpect(jsonPath("$.blocks[1].fields.length()").value(10))
				.andExpect(jsonPath("$.blocks[1].fields[9].text").value("*field9*\n9"))
				.andExpect(jsonPath("$.blocks[2].type").value("section"))
				.andExpect(jsonPath("$.blocks[2].fields.length()").value(1))
				.andExpect(jsonPath("$.blocks[2].fields[0].text").value("*field10*\n10"))
				.andExpect(jsonPath("$.blocks[3].type").value("context"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		sender().send(alert("신규 가입", fields));

		server.verify();
	}

	@Test
	void 필드_값이_500자를_넘으면_잘라서_보낸다() {
		String longValue = "a".repeat(600);
		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(jsonPath("$.blocks[1].fields[0].text").value("*detail*\n" + "a".repeat(499) + "…"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		sender().send(alert("영상 분석 지연", List.of(new AlertField("detail", longValue))));

		server.verify();
	}

	@Test
	void 활성_프로파일이_없으면_기본_프로파일을_환경_이름으로_쓴다() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("local");
		server.expect(requestTo(WEBHOOK_URL))
				.andExpect(jsonPath("$.blocks[2].elements[0].text").value("local · 2026-11-02 21:34:56 KST"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		sender(environment).send(alert("신규 가입", FIELDS));

		server.verify();
	}

	@Test
	void Slack이_4xx로_응답하면_전송_실패_예외를_던진다() {
		server.expect(requestTo(WEBHOOK_URL)).andRespond(withResourceNotFound());

		assertThatThrownBy(() -> sender().send(alert("신규 가입", FIELDS)))
				.isInstanceOf(AlertDeliveryFailedException.class)
				.hasMessage("status=404");
	}

	@Test
	void Slack이_5xx로_응답하면_전송_실패_예외를_던진다() {
		server.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> sender().send(alert("신규 가입", FIELDS)))
				.isInstanceOf(AlertDeliveryFailedException.class)
				.hasMessage("status=500");
	}

	@Test
	void Slack에_연결하지_못하면_전송_실패_예외를_던진다() {
		server.expect(requestTo(WEBHOOK_URL)).andRespond(withException(new IOException("connection refused")));

		assertThatThrownBy(() -> sender().send(alert("신규 가입", FIELDS)))
				.isInstanceOf(AlertDeliveryFailedException.class)
				.hasMessage("reason=ResourceAccessException");
	}

	@Test
	void 전송_실패_예외에_webhook_URL을_담지_않는다() {
		server.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> sender().send(alert("신규 가입", FIELDS)))
				.hasMessageNotContaining(WEBHOOK_URL)
				.hasNoCause();
	}
}
