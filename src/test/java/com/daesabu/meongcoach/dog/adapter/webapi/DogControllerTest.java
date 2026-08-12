package com.daesabu.meongcoach.dog.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageFinder;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 강아지 프로필 이미지 조회 API 검증.
 */
@WebMvcTest(DogController.class)
@AutoConfigureRestDocs
@DisplayName("강아지 프로필 이미지 조회 API")
class DogControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DogProfileImageFinder dogProfileImageFinder;

	@Test
	@DisplayName("강아지의 프로필 이미지 URL을 반환한다")
	void findProfileImageReturnsImageUrl() throws Exception {
		given(dogProfileImageFinder.findProfileImageUrl(42L, 10L)).willReturn(IMAGE_URL);

		mockMvc.perform(get("/api/dogs/{dogId}/profile-image", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profileImageUrl").value(IMAGE_URL))
				.andDo(document("dog/profile-image",
						pathParameters(
								parameterWithName("dogId").description("조회할 강아지 ID")
						),
						responseFields(
								fieldWithPath("profileImageUrl")
										.description("강아지 프로필 이미지 URL. 등록하지 않았으면 빈 문자열")
						)
				));
	}

	@Test
	@DisplayName("로그인한 사용자 ID로 강아지 프로필 이미지를 조회한다")
	void findProfileImageWithCurrentUserId() throws Exception {
		given(dogProfileImageFinder.findProfileImageUrl(42L, 10L)).willReturn(IMAGE_URL);

		mockMvc.perform(get("/api/dogs/{dogId}/profile-image", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(dogProfileImageFinder).should().findProfileImageUrl(42L, 10L);
	}

	@Test
	@DisplayName("프로필 이미지가 없는 강아지는 빈 문자열과 200을 반환한다")
	void findProfileImageReturnsEmptyStringWhenImageIsAbsent() throws Exception {
		given(dogProfileImageFinder.findProfileImageUrl(42L, 10L)).willReturn("");

		mockMvc.perform(get("/api/dogs/{dogId}/profile-image", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profileImageUrl").value(""));
	}

	@Test
	@DisplayName("없거나 본인 소유가 아닌 강아지면 404와 에러 코드를 반환한다")
	void findProfileImageReturnsNotFoundWhenDogIsNotOwned() throws Exception {
		given(dogProfileImageFinder.findProfileImageUrl(42L, 999L)).willThrow(new DogNotFoundException(999L));

		mockMvc.perform(get("/api/dogs/{dogId}/profile-image", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 강아지를 찾을 수 없습니다."))
				.andDo(document("dog/profile-image-error",
						pathParameters(
								parameterWithName("dogId").description("조회할 강아지 ID")
						),
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
	@DisplayName("인증 정보가 없으면 401을 반환한다")
	void findProfileImageReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/dogs/{dogId}/profile-image", 10L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
