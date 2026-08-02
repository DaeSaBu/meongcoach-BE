package com.daesabu.meongcoach.ai.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.ai.application.provided.AiReportDetailView;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportView;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AI 리포트 목록·상세 조회 API 검증.
 */
@WebMvcTest(AiReportController.class)
@AutoConfigureRestDocs
@DisplayName("AI 리포트 조회 API")
class AiReportControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다
	// (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiReportFinder aiReportFinder;

	@Test
	@DisplayName("리포트 목록을 최신순으로 반환한다")
	void findReportsReturnsReportsLatestFirst() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of(
				new AiReportView(11L, "videos/training/42/second.mp4", LocalDateTime.of(2026, 8, 2, 10, 30, 0)),
				new AiReportView(10L, "videos/training/42/first.mp4", LocalDateTime.of(2026, 8, 1, 9, 0, 0))
		));

		mockMvc.perform(get("/api/ai/reports").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reports[0].reportId").value(11))
				.andExpect(jsonPath("$.reports[0].videoObjectKey").value("videos/training/42/second.mp4"))
				.andExpect(jsonPath("$.reports[0].createdAt").value("2026-08-02T10:30:00"))
				.andExpect(jsonPath("$.reports[1].reportId").value(10))
				.andExpect(jsonPath("$.reports[1].videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.reports[1].createdAt").value("2026-08-01T09:00:00"))
				.andDo(document("ai/report-list",
						responseFields(
								fieldWithPath("reports[]").description("사용자의 AI 리포트 목록. 생성 시각 내림차순"),
								fieldWithPath("reports[].reportId").description("리포트 ID"),
								fieldWithPath("reports[].videoObjectKey").description("분석한 영상의 S3 객체 키"),
								fieldWithPath("reports[].createdAt").description("리포트 생성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	@DisplayName("리포트가 없으면 빈 배열과 200을 반환한다")
	void findReportsReturnsEmptyArrayWhenNoReportExists() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/ai/reports").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reports").isArray())
				.andExpect(jsonPath("$.reports").isEmpty());
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 리포트 목록 조회를 위임한다")
	void findReportsDelegatesWithCurrentUserId() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/ai/reports").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiReportFinder).should().findReports(42L);
	}

	@Test
	@DisplayName("인증 정보가 없으면 401을 반환한다")
	void findReportsReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/ai/reports"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("리포트 하나를 본문과 함께 반환한다")
	void findReportReturnsReportWithContent() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailView(10L,
				"videos/training/42/first.mp4", "분리불안 징후가 관찰됩니다.", LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reportId").value(10))
				.andExpect(jsonPath("$.videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.content").value("분리불안 징후가 관찰됩니다."))
				.andExpect(jsonPath("$.createdAt").value("2026-08-01T09:00:00"))
				.andDo(document("ai/report-detail",
						pathParameters(
								parameterWithName("reportId").description("조회할 리포트 ID")
						),
						responseFields(
								fieldWithPath("reportId").description("리포트 ID"),
								fieldWithPath("videoObjectKey").description("분석한 영상의 S3 객체 키"),
								fieldWithPath("content").description("AI 분석 리포트 본문"),
								fieldWithPath("createdAt").description("리포트 생성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 리포트 상세 조회를 위임한다")
	void findReportDelegatesWithCurrentUserId() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailView(10L,
				"videos/training/42/first.mp4", "분리불안 징후가 관찰됩니다.", LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiReportFinder).should().findReport(42L, 10L);
	}

	@Test
	@DisplayName("없거나 본인 소유가 아닌 리포트면 404와 에러 코드를 반환한다")
	void findReportReturnsNotFoundWhenReportDoesNotExistOrIsNotOwned() throws Exception {
		given(aiReportFinder.findReport(42L, 999L)).willThrow(new AiReportNotFoundException(999L));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 999L).principal(CURRENT_USER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("AI_REPORT_NOT_FOUND"))
				.andDo(document("ai/report-detail-error",
						pathParameters(
								parameterWithName("reportId").description("조회할 리포트 ID")
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
}
