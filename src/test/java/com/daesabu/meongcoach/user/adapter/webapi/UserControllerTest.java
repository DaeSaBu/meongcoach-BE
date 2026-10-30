package com.daesabu.meongcoach.user.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.security.Principal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserFinder userFinder;

	@Test
	void 온보딩_전_회원이_내_정보를_조회하면_온보딩이_필요하고_프로필은_null로_응답한다() throws Exception {
		User user = User.registerUser();
		ReflectionTestUtils.setField(user, "id", 42L);
		given(userFinder.findById(42L)).willReturn(user);

		mockMvc.perform(get("/api/users/me").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.role").value("ONBOARDING_USER"))
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andExpect(jsonPath("$.profile").value(nullValue()));
	}

	@Test
	void 정회원이_내_정보를_조회하면_회원_ID와_프로필을_응답한다() throws Exception {
		User user = User.registerUser();
		ReflectionTestUtils.setField(user, "id", 42L);
		user.completeOnboarding(new UserProfileCreateCommand(
				"멍멍이집사", "https://cdn.example.com/profile.png", "INFP", Set.of(), Set.of()));
		given(userFinder.findById(42L)).willReturn(user);

		mockMvc.perform(get("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.needsOnboarding").value(false))
				.andExpect(jsonPath("$.profile.nickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.profile.profileImageUrl").value("https://cdn.example.com/profile.png"))
				.andDo(document("user/me",
						responseFields(
								fieldWithPath("userId").description("회원 ID"),
								fieldWithPath("role").description("회원 역할. USER, ONBOARDING_USER, ADMIN, EXPERT 중 하나"),
								fieldWithPath("needsOnboarding").description("온보딩 화면으로 보내야 하는지 여부"),
								fieldWithPath("profile").optional().description("회원 프로필. 온보딩 전이면 null"),
								fieldWithPath("profile.nickname").description("닉네임"),
								fieldWithPath("profile.profileImageUrl").description("프로필 이미지 URL. 등록하지 않았으면 빈 문자열")
						)
				));
	}

	@Test
	void 없는_회원이면_404와_에러_코드를_반환한다() throws Exception {
		given(userFinder.findById(42L)).willThrow(new UserNotFoundException(42L));

		mockMvc.perform(get("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andDo(document("user/me-error",
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
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());

		then(userFinder).shouldHaveNoInteractions();
	}
}
