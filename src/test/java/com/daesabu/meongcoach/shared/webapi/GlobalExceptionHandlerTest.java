package com.daesabu.meongcoach.shared.webapi;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.daesabu.meongcoach.shared.exception.DomainException;
import com.daesabu.meongcoach.shared.exception.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@Import(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@AutoConfigureRestDocs
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
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
	void unknownPathReturnsNotFoundProblem() throws Exception {
		mockMvc.perform(get("/test/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
		mockMvc.perform(post("/test/domain-error"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{invalid-json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void unexpectedExceptionReturnsInternalServerErrorWithoutInternalDetail() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
	}

	enum TestErrorCode implements ErrorCode {
		TEST_NOT_FOUND(404, "테스트 리소스를 찾을 수 없습니다.");

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

	record TestRequest(@NotBlank String name) {
	}

	@RestController
	static class ExceptionTriggerController {

		@GetMapping("/test/domain-error")
		void domainError() {
			throw new TestNotFoundException();
		}

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("내부 구현 정보가 담긴 메시지");
		}
	}
}
