package com.daesabu.meongcoach.notification.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.notification.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;
import com.daesabu.meongcoach.notification.domain.exception.ChannelTalkUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupportChatSessionController.class)
@AutoConfigureRestDocs
class SupportChatSessionControllerTest {
	@Autowired MockMvc mvc;
	@MockitoBean SupportChatSessionFinder sessions;
	@Test
	void 로그인_주체의_상담_식별값만_반환한다() throws Exception {
		given(sessions.find(42L)).willReturn(new SupportChatSessionResult("development-42", "signed-member-hash"));

		mvc.perform(get("/api/notifications/support-session").principal(() -> "42")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value("development-42"))
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
