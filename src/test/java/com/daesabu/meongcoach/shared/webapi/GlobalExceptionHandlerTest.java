package com.daesabu.meongcoach.shared.webapi;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.shared.exception.DomainException;
import com.daesabu.meongcoach.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@Import(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@AutoConfigureRestDocs
@DisplayName("전역 예외 처리")
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("도메인 예외는 에러 코드가 담긴 ProblemDetail을 반환한다")
	void domainExceptionReturnsProblemDetailWithErrorCode() throws Exception {
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
	@DisplayName("검증에 실패하면 필드 에러 목록을 반환한다")
	void validationFailureReturnsFieldErrors() throws Exception {
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
	@DisplayName("서버 측 도메인 예외도 에러 코드가 담긴 ProblemDetail을 반환한다")
	void serverSideDomainExceptionReturnsProblemDetailWithErrorCode() throws Exception {
		mockMvc.perform(get("/test/domain-error-5xx"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.code").value("TEST_INTERNAL"))
				.andExpect(jsonPath("$.detail").value("테스트 처리 중 서버 오류가 발생했습니다."));
	}

	@Test
	@DisplayName("알 수 없는 경로는 NOT_FOUND를 반환한다")
	void unknownPathReturnsNotFoundProblem() throws Exception {
		mockMvc.perform(get("/test/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("지원하지 않는 HTTP 메서드는 METHOD_NOT_ALLOWED를 반환한다")
	void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
		mockMvc.perform(post("/test/domain-error"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("잘못된 형식의 JSON은 BAD_REQUEST를 반환한다")
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{invalid-json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	@DisplayName("예상치 못한 예외는 내부 정보 노출 없이 INTERNAL_SERVER_ERROR를 반환한다")
	void unexpectedExceptionReturnsInternalServerErrorWithoutInternalDetail() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
	}

	@Test
	@DisplayName("인증 실패는 원인 노출 없이 UNAUTHORIZED를 반환한다")
	void authenticationFailureReturnsUnauthorized() throws Exception {
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
	@DisplayName("권한 부족은 FORBIDDEN을 반환한다")
	void accessDeniedReturnsForbidden() throws Exception {
		mockMvc.perform(get("/test/access-denied"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."));
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
