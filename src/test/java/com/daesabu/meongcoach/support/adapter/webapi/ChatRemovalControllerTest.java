package com.daesabu.meongcoach.support.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.daesabu.meongcoach.support.application.provided.ChatRemover;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatRemovalController.class)
@AutoConfigureRestDocs
class ChatRemovalControllerTest {
	@Autowired MockMvc mvc;
	@MockitoBean ChatRemover remover;

	@Test
	void 인증_회원의_대화를_종료하고_204를_반환한다() throws Exception {
		mvc.perform(delete("/api/support/conversations/{id}", 17)
				.principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""))
				.andDo(document("support/chat-leave",
						pathParameters(parameterWithName("id").description("종료할 대화 ID"))));

		verify(remover).leave(42L, 17);
	}

	@Test
	void 다른_회원의_대화는_403을_반환한다() throws Exception {
		doThrow(new ChatAccessDeniedException()).when(remover).leave(42L, 17);

		mvc.perform(delete("/api/support/conversations/{id}", 17)
				.principal(() -> "42").header("Authorization", "Bearer access-token"))
				.andExpect(status().isForbidden())
				.andDo(document("support/chat-leave-error",
						pathParameters(parameterWithName("id").description("종료할 대화 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태"),
								fieldWithPath("status").description("상태 코드"),
								fieldWithPath("detail").description("설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("에러 코드"),
								fieldWithPath("timestamp").description("발생 시각"))));
	}

	@Test
	void 잘못된_대화_ID는_종료하지_않는다() throws Exception {
		mvc.perform(delete("/api/support/conversations/0").principal(() -> "42"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(remover);
	}
}
