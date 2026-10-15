package com.daesabu.meongcoach.graduation.adapter.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.graduation.application.required.SubmissionNotice;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackSubmissionNotifierTest {

	private static final String WEBHOOK_URL = "https://hooks.slack.test/services/T000/B000/XXXX";
	private static final SubmissionNotice NOTICE =
			new SubmissionNotice(31L, 42L, 7L, "분리불안", 2, Instant.parse("2026-10-14T05:03:00Z"));

	@Test
	void 제출_한_건을_한_줄_메시지로_웹훅에_보낸다() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SlackSubmissionNotifier notifier = new SlackSubmissionNotifier(new GraduationSlackProperties(WEBHOOK_URL), builder);
		server.expect(once(), requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.text")
						.value("[실습 인증 제출] 토픽 #7 분리불안 · userId 42 · submissionId 31 · 2번째 제출 · 2026-10-14T14:03"))
				.andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

		notifier.notifySubmitted(NOTICE);

		server.verify();
	}

	@Test
	void 웹훅_전송이_실패해도_예외를_던지지_않는다() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SlackSubmissionNotifier notifier = new SlackSubmissionNotifier(new GraduationSlackProperties(WEBHOOK_URL), builder);
		server.expect(once(), requestTo(WEBHOOK_URL)).andRespond(withServerError());

		assertThatCode(() -> notifier.notifySubmitted(NOTICE)).doesNotThrowAnyException();
		server.verify();
	}

	@Test
	void 웹훅_주소가_비어_있으면_보내지_않는다() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SlackSubmissionNotifier notifier = new SlackSubmissionNotifier(new GraduationSlackProperties(""), builder);

		notifier.notifySubmitted(NOTICE);

		server.verify();
	}
}
