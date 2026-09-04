package com.daesabu.meongcoach.shared.webapi;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.shared.exception.DomainException;
import com.daesabu.meongcoach.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@Import(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@AutoConfigureRestDocs
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 도메인_예외는_에러_코드가_담긴_ProblemDetail을_반환한다() throws Exception {
		mockMvc.perform(get("/test/domain-error"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TEST_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("테스트 리소스를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.timestamp").exists())
				.andDo(document("shared/error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드. `{모듈}_{원인}` 형식"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 검증에_실패하면_필드_에러_목록을_반환한다() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andDo(document("shared/error-validation",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)"),
								fieldWithPath("errors[].field").description("검증에 실패한 필드 이름"),
								fieldWithPath("errors[].message").description("검증 실패 사유")
						)
				));
	}

	@Test
	void 서버_측_도메인_예외도_에러_코드가_담긴_ProblemDetail을_반환한다() throws Exception {
		mockMvc.perform(get("/test/domain-error-5xx"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.code").value("TEST_INTERNAL"))
				.andExpect(jsonPath("$.detail").value("테스트 처리 중 서버 오류가 발생했습니다."));
	}

	@Test
	void 알_수_없는_경로는_NOT_FOUND를_반환한다() throws Exception {
		mockMvc.perform(get("/test/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 지원하지_않는_HTTP_메서드는_METHOD_NOT_ALLOWED를_반환한다() throws Exception {
		mockMvc.perform(post("/test/domain-error"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void 잘못된_형식의_JSON은_BAD_REQUEST를_반환한다() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{invalid-json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void 예상치_못한_예외는_내부_정보_노출_없이_INTERNAL_SERVER_ERROR를_반환한다() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
	}

	@Test
	void 인증_실패는_원인_노출_없이_UNAUTHORIZED를_반환한다() throws Exception {
		mockMvc.perform(get("/test/authentication-error"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
				.andExpect(jsonPath("$.timestamp").exists())
				.andDo(document("shared/error-unauthorized",
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
	void 권한_부족은_FORBIDDEN을_반환한다() throws Exception {
		mockMvc.perform(get("/test/access-denied"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."));
	}

	// 필터 체인 전용 에러라 컨트롤러 슬라이스에서 재현할 수 없어, 실제 흐름과 같은
	// SecurityContext 권한을 테스트 스레드에 심어 재현한다 (MockMvc는 동일 스레드에서 실행된다)
	@Test
	void 온보딩_미완료_회원의_권한_부족은_ONBOARDING_NOT_COMPLETED를_반환한다() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("1", null, "ROLE_ONBOARDING_MEMBER"));

		mockMvc.perform(get("/test/access-denied"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"))
				.andExpect(jsonPath("$.detail").value("온보딩을 완료해야 이용할 수 있는 기능입니다."))
				.andDo(document("shared/error-onboarding-not-completed",
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

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	enum TestErrorCode implements ErrorCode {
		TEST_NOT_FOUND(404, "테스트 리소스를 찾을 수 없습니다."),
		TEST_INTERNAL(500, "테스트 처리 중 서버 오류가 발생했습니다.");

		private final int status;
		private final String message;

		TestErrorCode(int status, String message) {
			this.status = status;
			this.message = message;
		}

		@Override
		public String code() {
			return name();
		}

		@Override
		public String message() {
			return message;
		}

		@Override
		public int status() {
			return status;
		}
	}

	static class TestNotFoundException extends DomainException {

		TestNotFoundException() {
			super(TestErrorCode.TEST_NOT_FOUND);
		}
	}

	static class TestInternalException extends DomainException {

		TestInternalException() {
			super(TestErrorCode.TEST_INTERNAL);
		}
	}

	record TestRequest(@NotBlank String name) {
	}

	@RestController
	static class ExceptionTriggerController {

		@GetMapping("/test/domain-error")
		void domainError() {
			throw new TestNotFoundException();
		}

		@GetMapping("/test/domain-error-5xx")
		void domainError5xx() {
			throw new TestInternalException();
		}

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("내부 구현 정보가 담긴 메시지");
		}

		// 실제로는 시큐리티 필터 체인이 던지고 SecurityExceptionTranslator가 여기로 되돌린다
		@GetMapping("/test/authentication-error")
		void authenticationError() {
			throw new BadCredentialsException("자격증명이 올바르지 않습니다");
		}

		@GetMapping("/test/access-denied")
		void accessDenied() {
			throw new AccessDeniedException("권한이 없습니다");
		}
	}
}
