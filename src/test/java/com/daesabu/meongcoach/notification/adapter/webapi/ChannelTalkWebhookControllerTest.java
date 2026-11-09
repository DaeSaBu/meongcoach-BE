package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.ChannelTalkProperties;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelTalkWebhookController.class)
@AutoConfigureRestDocs
@EnableConfigurationProperties(ChannelTalkProperties.class)
class ChannelTalkWebhookControllerTest {

	private static final String WEBHOOK_PATH = "/webhooks/channel-talk";
	private static final String TOKEN = "test-channel-talk-webhook-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventNotificationDispatcher dispatcher;

	@Test
	void 운영자가_답하면_답변을_받은_회원에게_알림을_요청한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event":"push","type":"message",
								"entity":{"chatType":"userChat","personType":"manager","plainText":"안녕하세요, 훈련사입니다."},
								"refers":{"user":{"memberId":"development-42"}}}
								"""))
				.andExpect(status().isOk())
				.andDo(document("notification/channel-talk-webhook",
						queryParameters(parameterWithName("token").description("채널톡 데스크에 등록한 웹훅 주소의 토큰")),
						relaxedRequestFields(fieldWithPath("event").description("이벤트 동작. push만 처리"), fieldWithPath("type").description("이벤트 종류. message만 처리"),
								fieldWithPath("entity.chatType").description("채팅 종류. userChat만 처리"),
								fieldWithPath("entity.personType").description("보낸 사람 종류. manager만 처리"),
								fieldWithPath("entity.plainText").description("메시지 본문. 알림 본문에 앞 100자까지 쓴다"),
								fieldWithPath("refers.user.memberId").description("앱이 채널톡을 부팅할 때 넘긴 {환경}-{회원 ID}"))));

		then(dispatcher).should().dispatch(new EventNotificationDispatchRequest("SUPPORT_REPLY", "USERS", List.of(42L),
				"문의하신 내용에 답변이 왔어요", "안녕하세요, 훈련사입니다.", "meongcoach://support-chat"));
	}

	@Test
	void 고객이_보낸_메시지면_알림을_요청하지_않고_200을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event":"push","type":"message","entity":{"chatType":"userChat","personType":"user","plainText":"문의드려요"},
								"refers":{"user":{"memberId":"development-42"}}}
								"""))
				.andExpect(status().isOk());

		then(dispatcher).shouldHaveNoInteractions();
	}

	@Test
	void 다른_환경의_고객이면_알림을_요청하지_않고_200을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event":"push","type":"message","entity":{"chatType":"userChat","personType":"manager","plainText":"답변"},
								"refers":{"user":{"memberId":"production-42"}}}
								"""))
				.andExpect(status().isOk());

		then(dispatcher).shouldHaveNoInteractions();
	}

	@Test
	void 토큰이_다르면_401을_반환한다() throws Exception {
		mockMvc.perform(post(WEBHOOK_PATH)
						.queryParam("token", "wrong-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event":"push","type":"message","entity":{"chatType":"userChat","personType":"manager","plainText":"답변"},
								"refers":{"user":{"memberId":"development-42"}}}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(dispatcher).shouldHaveNoInteractions();
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

		then(dispatcher).shouldHaveNoInteractions();
	}
}
