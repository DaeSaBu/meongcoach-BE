package com.daesabu.meongcoach.loadtest.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.LocalAccountRegister;
import com.daesabu.meongcoach.user.domain.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// 컨트롤러는 meongcoach.loadtest.enabled가 true일 때만 등록되므로 슬라이스에도 같은 프로퍼티를 준다
@WebMvcTest(value = LoadTestAccountController.class,
		properties = {"meongcoach.loadtest.enabled=true", "meongcoach.loadtest.api-key=" + LoadTestAccountControllerTest.API_KEY})
@Import(LoadTestAccountControllerTest.StubConfig.class)
@AutoConfigureRestDocs
class LoadTestAccountControllerTest {

	static final String API_KEY = "test-loadtest-key";

	private static final String API_KEY_HEADER = "X-Loadtest-Key";
	private static final String EMAIL = "lt-0001@meongcoach.test";
	private static final String DUPLICATE_EMAIL = "duplicate@meongcoach.test";
	private static final String PASSWORD = "loadtest-password";
	private static final long CREATED_USER_ID = 42L;

	@Autowired
	private MockMvc mockMvc;

	private static String body(String email, String password) {
		return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
	}

	@Test
	void 키가_일치하면_계정을_만들고_201과_회원_ID를_반환한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.header(API_KEY_HEADER, API_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(EMAIL, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(CREATED_USER_ID))
				.andDo(document("loadtest/create-account",
						requestHeaders(
								headerWithName(API_KEY_HEADER).description("부하 테스트 공유 키(LOADTEST_API_KEY)")
						),
						requestFields(
								fieldWithPath("email").description("필수 입력. 생성할 계정의 이메일. 이미 등록된 이메일이면 409"),
								fieldWithPath("password").description("필수 입력. 8자 이상 72자 이하 평문 비밀번호")
						),
						responseFields(
								fieldWithPath("userId").description("생성된 회원 ID. 역할은 온보딩 전(ONBOARDING_MEMBER)")
						)
				));
	}

	@Test
	void 키가_일치하지_않으면_403을_반환한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.header(API_KEY_HEADER, "wrong-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("LOADTEST_INVALID_KEY"))
				.andDo(document("loadtest/create-account-error",
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
	void 키_헤더가_없으면_403을_반환한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("LOADTEST_INVALID_KEY"));
	}

	@Test
	void 이미_등록된_이메일이면_409를_반환한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.header(API_KEY_HEADER, API_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(DUPLICATE_EMAIL, PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_DUPLICATE_EMAIL"));
	}

	@Test
	void 비밀번호가_8자_미만이면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.header(API_KEY_HEADER, API_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(EMAIL, "short")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("password"));
	}

	// 슬라이스에는 @ConfigurationPropertiesScan이 적용되지 않으므로 키 프로퍼티 바인딩을 직접 켠다
	@TestConfiguration
	@EnableConfigurationProperties(LoadTestProperties.class)
	static class StubConfig {

		@Bean
		LocalAccountRegister localAccountRegister() {
			return info -> {
				if (DUPLICATE_EMAIL.equals(info.email())) {
					throw new DuplicateEmailException();
				}
				return CREATED_USER_ID;
			};
		}
	}
}
