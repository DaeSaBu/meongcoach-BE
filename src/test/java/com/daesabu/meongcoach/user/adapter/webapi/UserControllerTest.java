package com.daesabu.meongcoach.user.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserWithdrawer userWithdrawer;

	@Test
	void 탈퇴하면_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNoContent())
				.andDo(document("user/withdraw"));
	}

	@Test
	void 인증_주체의_회원_ID로_탈퇴를_위임한다() throws Exception {
		mockMvc.perform(delete("/api/users/me").principal(CURRENT_USER))
				.andExpect(status().isNoContent());

		then(userWithdrawer).should().withdraw(42L);
	}

	@Test
	void 없는_회원이면_404와_에러_코드를_반환한다() throws Exception {
		willThrow(new UserNotFoundException(42L)).given(userWithdrawer).withdraw(42L);

		mockMvc.perform(delete("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 42인 회원을 찾을 수 없습니다."))
				.andDo(document("user/withdraw-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/api/users/me"))
				.andExpect(status().isUnauthorized());

		then(userWithdrawer).shouldHaveNoInteractions();
	}
}
