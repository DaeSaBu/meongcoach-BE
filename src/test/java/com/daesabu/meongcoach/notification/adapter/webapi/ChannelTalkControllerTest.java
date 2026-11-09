package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.notification.application.provided.SupportReplyReceiver;
import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;
import com.daesabu.meongcoach.notification.domain.exception.ChannelTalkUnavailableException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidChannelTalkTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelTalkController.class)
@AutoConfigureRestDocs
class ChannelTalkControllerTest {
	private static final String BODY = """
			{"event":"push","type":"message","entity":{
			"id":"reply-1","chatType":"userChat","personType":"manager","options":[],"plainText":"답변"},
			"refers":{"user":{"id":"channel-user-1","memberId":"dev-42"},"userChat":{"id":"chat-1"}}}
			""";

	@Autowired MockMvc mvc;
	@MockitoBean SupportReplyReceiver receiver;
	@MockitoBean SupportChatSessionFinder sessions;

	@Test
	void 외부_필드가_추가된_웹훅을_받으면_토큰과_함께_위임한다() throws Exception {
		mvc.perform(post("/webhooks/channel-talk").queryParam("token", "webhook-token")
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isNoContent())
				.andDo(document("notification/support-reply",
						queryParameters(parameterWithName("token").description("채널톡 웹훅 토큰")),
						requestFields(fieldWithPath("event").description("push"), fieldWithPath("type").description("message"),
								subsectionWithPath("entity").description("상담원 메시지. id·chatType·personType·options·broadcastedMsg·marketing으로 발송 여부 판정"),
								subsectionWithPath("refers").description("refers.user.memberId에 환경-회원 ID를 전달"))));

		then(receiver).should().receive(eq("webhook-token"), any(ChannelTalkWebhookRequest.class));
	}

	@Test
	void 잘못된_웹훅_토큰은_401을_반환한다() throws Exception {
		willThrow(new InvalidChannelTalkTokenException()).given(receiver).receive(eq("wrong"), any());

		mvc.perform(post("/webhooks/channel-talk").queryParam("token", "wrong")
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_CHANNEL_TALK_TOKEN_INVALID"))
				.andDo(document("notification/support-reply-error", responseFields(
						fieldWithPath("title").description("HTTP 상태 이름"), fieldWithPath("status").description("HTTP 상태 코드"),
						fieldWithPath("detail").description("에러 설명"), fieldWithPath("instance").description("요청 경로"),
						fieldWithPath("code").description("에러 코드"), fieldWithPath("timestamp").description("발생 시각"))));
	}

	@Test
	void 로그인_주체의_상담_식별값만_반환한다() throws Exception {
		given(sessions.find(42L)).willReturn(new SupportChatSessionResult("dev-42", "signed-member-hash"));

		mvc.perform(get("/api/notifications/support-session").principal(() -> "42")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value("dev-42"))
				.andExpect(jsonPath("$.memberHash").value("signed-member-hash"))
				.andDo(document("notification/support-session", responseFields(
						fieldWithPath("memberId").description("환경-회원 ID. dev·prod·local 구분"),
						fieldWithPath("memberHash").description("채널톡 회원 해시. 서버의 16진수 비밀 키로 HMAC-SHA256 서명"))));
	}

	@Test
	void 해시_키가_없으면_503을_반환한다() throws Exception {
		given(sessions.find(42L)).willThrow(new ChannelTalkUnavailableException());

		mvc.perform(get("/api/notifications/support-session").principal(() -> "42"))
				.andExpect(status().isServiceUnavailable())
				.andDo(document("notification/support-session-error", responseFields(
						fieldWithPath("title").description("HTTP 상태 이름"), fieldWithPath("status").description("HTTP 상태 코드"),
						fieldWithPath("detail").description("에러 설명"), fieldWithPath("instance").description("요청 경로"),
						fieldWithPath("code").description("에러 코드"), fieldWithPath("timestamp").description("발생 시각"))));
	}

	@Test
	void 로그인_주체가_없으면_상담_식별값을_반환하지_않는다() throws Exception {
		mvc.perform(get("/api/notifications/support-session"))
				.andExpect(status().isUnauthorized());

		then(sessions).shouldHaveNoInteractions();
	}
}
