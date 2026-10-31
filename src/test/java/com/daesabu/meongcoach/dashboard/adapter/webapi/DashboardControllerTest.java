package com.daesabu.meongcoach.dashboard.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.dashboard.application.provided.DashboardUserFinder;
import com.daesabu.meongcoach.dashboard.domain.exception.DashboardUserNotFoundException;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@AutoConfigureRestDocs
class DashboardControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DashboardUserFinder dashboardUserFinder;

	@Test
	void 내_정보를_조회하면_회원_ID와_역할을_응답한다() throws Exception {
		given(dashboardUserFinder.findRole(42L)).willReturn(AuthorityRole.ADMIN);

		mockMvc.perform(get("/api/dashboard/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.userRole").value("ADMIN"))
				.andDo(document("dashboard/me",
						responseFields(
								fieldWithPath("userId").description("회원 ID"),
								fieldWithPath("userRole").description("회원 역할. 대시보드에서는 ADMIN 또는 EXPERT")
						)
				));
	}

	@Test
	void 없는_회원이면_404와_에러_코드를_반환한다() throws Exception {
		given(dashboardUserFinder.findRole(42L)).willThrow(new DashboardUserNotFoundException(42L));

		mockMvc.perform(get("/api/dashboard/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DASHBOARD_USER_NOT_FOUND"))
				.andDo(document("dashboard/me-error",
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
		mockMvc.perform(get("/api/dashboard/me"))
				.andExpect(status().isUnauthorized());

		then(dashboardUserFinder).shouldHaveNoInteractions();
	}
}
