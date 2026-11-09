package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.SupportReplyNotifier;
import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelTalkWebhookController.class)
@AutoConfigureRestDocs
class ChannelTalkWebhookControllerTest {

	private static final String WEBHOOK_PATH = "/webhooks/channel-talk";
	private static final String TOKEN = "test-channel-talk-webhook-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupportReplyNotifier notifier;

	@Test
	void 웹훅_JSON을_요청_객체로_변환해_알림_서비스에_전달한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event":"push","type":"message",
								"entity":{"chatType":"userChat","personType":"manager","plainText":"안녕하세요, 훈련사입니다."},
								"refers":{"user":{"memberId":"42"}}}
								"""))
				.andExpect(status().isOk())
				.andDo(document("notification/channel-talk-webhook",
						queryParameters(parameterWithName("token").description("채널톡 데스크에 등록한 웹훅 주소의 토큰")),
						relaxedRequestFields(fieldWithPath("event").description("이벤트 동작. push만 처리"), fieldWithPath("type").description("이벤트 종류. message만 처리"),
								fieldWithPath("entity.chatType").description("채팅 종류. userChat만 처리"),
								fieldWithPath("entity.personType").description("보낸 사람 종류. manager만 처리"),
								fieldWithPath("entity.plainText").description("메시지 본문. 알림 본문에 앞 100자까지 쓴다"),
								fieldWithPath("refers.user.memberId").description("앱이 채널톡을 부팅할 때 넘긴 회원 ID 문자열"))));

		then(notifier).should().notifyReply(new ChannelTalkWebhookRequest("push", "message",
				new ChannelTalkWebhookRequest.Entity("userChat", "manager", "안녕하세요, 훈련사입니다.", null, null, null),
				new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User("42"))));
	}

	@Test
	void 본문이_JSON이_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("not-json"))
				.andExpect(status().isBadRequest())
				.andDo(document("notification/channel-talk-webhook-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("에러 코드"),
								fieldWithPath("timestamp").description("발생 시각"))));

		then(notifier).shouldHaveNoInteractions();
	}
}
