package com.daesabu.meongcoach.loadtest.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.loadtest.application.provided.LoadTestRecorder;
import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import java.security.Principal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * INSERT 부하 측정용 기록 API 검증.
 */
@WebMvcTest(LoadTestRecordController.class)
@AutoConfigureRestDocs
class LoadTestRecordControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoadTestRecorder loadTestRecorder;

	// 영속화 없이 응답을 만들기 위해 DB가 채우는 id·createdAt을 직접 세팅한다
	private static LoadTestRecord savedRecord(Long id, Long userId) {
		LoadTestRecord record = LoadTestRecord.record(userId);
		ReflectionTestUtils.setField(record, "id", id);
		ReflectionTestUtils.setField(record, "createdAt", LocalDateTime.now());
		return record;
	}

	@Test
	void 저장된_기록을_201로_반환한다() throws Exception {
		given(loadTestRecorder.record(42L)).willReturn(savedRecord(1L, 42L));

		mockMvc.perform(post("/api/loadtest/records")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.userId").value(42))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andDo(document("loadtest/record",
						responseFields(
								fieldWithPath("id").description("저장된 기록 ID"),
								fieldWithPath("userId").description("기록을 남긴 회원 ID"),
								fieldWithPath("createdAt").description("기록 저장 시각")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_기록을_위임한다() throws Exception {
		given(loadTestRecorder.record(42L)).willReturn(savedRecord(7L, 42L));

		mockMvc.perform(post("/api/loadtest/records")
						.principal(CURRENT_USER))
				.andExpect(status().isCreated());

		then(loadTestRecorder).should().record(42L);
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/records"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andDo(document("loadtest/record-error",
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
}
