package com.daesabu.meongcoach.user.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdateRequest;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdater;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import com.daesabu.meongcoach.user.domain.exception.UserProfileNotFoundException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
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
	private UserFinder userFinder;

	@MockitoBean
	private UserProfileFinder userProfileFinder;

	@MockitoBean
	private UserProfileUpdater userProfileUpdater;

	private static final String UPDATE_REQUEST = """
			{
				"nickname": "새닉네임",
				"profileImageUrl": "https://images.test.meongcoach.com/images/user-profile/1/a.jpg",
				"birthDate": "1990-05-05",
				"mbti": "ENTJ",
				"gender": "MALE"
			}
			""";

	private static UserProfile profile() {
		return UserProfile.create(User.registerUser(), new UserProfileCreateCommand("멍멍이집사",
				"https://images.test.meongcoach.com/images/user-profile/1/a.jpg", LocalDate.of(1998, 1, 1), "INFP",
				"FEMALE", Set.of(2L, 1L), Set.of()));
	}

	private static List<FieldDescriptor> profileResponseFields() {
		return List.of(
				fieldWithPath("nickname").description("닉네임"),
				fieldWithPath("profileImageUrl").description("프로필 이미지 공개 URL. 미설정이면 빈 문자열"),
				fieldWithPath("birthDate").description("생년월일. 미입력이면 null").optional(),
				fieldWithPath("mbti").description("MBTI 코드. 미입력이면 null").optional(),
				fieldWithPath("gender").description("성별 `MALE`/`FEMALE`/`NONE`(응답 안 함). 미입력이면 null").optional(),
				fieldWithPath("priorTrainingTopicIds[]").description("이전에 교육한 토픽 ID 목록(오름차순)")
		);
	}

	private static List<FieldDescriptor> errorResponseFields() {
		return List.of(
				fieldWithPath("title").description("HTTP 상태 이름"),
				fieldWithPath("status").description("HTTP 상태 코드"),
				fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
				fieldWithPath("instance").description("에러가 발생한 요청 경로"),
				fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
				fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
		);
	}

	@Test
	void 온보딩_전_회원이_내_정보를_조회하면_온보딩이_필요하다고_응답한다() throws Exception {
		given(userFinder.findById(42L)).willReturn(User.registerUser());

		mockMvc.perform(get("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andDo(document("user/me",
						responseFields(
								fieldWithPath("needsOnboarding").description("온보딩 화면으로 보내야 하는지 여부")
						)
				));
	}

	@Test
	void 정회원이_내_정보를_조회하면_온보딩이_필요하지_않다고_응답한다() throws Exception {
		User user = User.registerUser();
		user.promoteToUser();
		given(userFinder.findById(42L)).willReturn(user);

		mockMvc.perform(get("/api/users/me").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.needsOnboarding").value(false));
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
	void 프로필을_조회한다() throws Exception {
		given(userProfileFinder.findProfile(42L)).willReturn(profile());

		mockMvc.perform(get("/api/users/profile")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("멍멍이집사"))
				.andExpect(jsonPath("$.mbti").value("INFP"))
				.andExpect(jsonPath("$.priorTrainingTopicIds[0]").value(1))
				.andExpect(jsonPath("$.priorTrainingTopicIds[1]").value(2))
				.andDo(document("user/profile",
						responseFields(profileResponseFields())
				));
	}

	@Test
	void 입력하지_않은_선택_값은_null로_조회한다() throws Exception {
		UserProfile profile = UserProfile.create(User.registerUser(),
				new UserProfileCreateCommand("멍멍이집사", null, null, null, null, Set.of(), Set.of()));
		given(userProfileFinder.findProfile(42L)).willReturn(profile);

		mockMvc.perform(get("/api/users/profile").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.birthDate").value(nullValue()))
				.andExpect(jsonPath("$.mbti").value(nullValue()))
				.andExpect(jsonPath("$.gender").value(nullValue()))
				.andExpect(jsonPath("$.priorTrainingTopicIds").isEmpty());
	}

	@Test
	void 프로필이_없으면_404와_에러_코드를_반환한다() throws Exception {
		given(userProfileFinder.findProfile(42L)).willThrow(new UserProfileNotFoundException(42L));

		mockMvc.perform(get("/api/users/profile")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_PROFILE_NOT_FOUND"))
				.andDo(document("user/profile-error",
						responseFields(errorResponseFields())
				));
	}

	@Test
	void 프로필을_수정하면_수정된_프로필을_반환한다() throws Exception {
		given(userProfileUpdater.update(eq(42L), any(UserProfileUpdateRequest.class))).willReturn(profile());

		mockMvc.perform(put("/api/users/profile")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("멍멍이집사"))
				.andDo(document("user/profile-update",
						requestFields(
								fieldWithPath("nickname").description("닉네임. 필수 입력, 최대 50자"),
								fieldWithPath("profileImageUrl").description(
										"프로필 이미지 공개 URL. 이미지 업로드 URL 발급 API의 publicUrl. 최대 512자, 선택 입력").optional(),
								fieldWithPath("birthDate").description("생년월일. 과거 날짜, 선택 입력").optional(),
								fieldWithPath("mbti").description("MBTI 코드. 선택 입력").optional(),
								fieldWithPath("gender").description(
										"성별. `MALE`/`FEMALE`/`NONE`(응답 안 함). 선택 입력").optional()
						),
						responseFields(profileResponseFields())
				));

		ArgumentCaptor<UserProfileUpdateRequest> captor = ArgumentCaptor.forClass(UserProfileUpdateRequest.class);
		then(userProfileUpdater).should().update(eq(42L), captor.capture());
		assertThat(captor.getValue()).isEqualTo(new UserProfileUpdateRequest("새닉네임",
				"https://images.test.meongcoach.com/images/user-profile/1/a.jpg", LocalDate.of(1990, 5, 5),
				"ENTJ", "MALE"));
	}

	@Test
	void 닉네임이_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(put("/api/users/profile")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_REQUEST.replace("새닉네임", " ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("nickname"))
				.andDo(document("user/profile-update-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)"),
								fieldWithPath("errors[].field").description("검증에 실패한 요청 필드"),
								fieldWithPath("errors[].message").description("검증 실패 사유")
						)
				));

		then(userProfileUpdater).shouldHaveNoInteractions();
	}

	@Test
	void 생년월일이_과거가_아니면_400을_반환한다() throws Exception {
		mockMvc.perform(put("/api/users/profile")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_REQUEST.replace("1990-05-05", "2999-01-01")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("birthDate"));
	}

	@Test
	void 유효하지_않은_MBTI면_400과_에러_코드를_반환한다() throws Exception {
		given(userProfileUpdater.update(eq(42L), any(UserProfileUpdateRequest.class)))
				.willThrow(new InvalidMbtiException("XXXX"));

		mockMvc.perform(put("/api/users/profile")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_REQUEST.replace("ENTJ", "XXXX")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_INVALID_MBTI"));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());

		then(userFinder).shouldHaveNoInteractions();
	}
}
