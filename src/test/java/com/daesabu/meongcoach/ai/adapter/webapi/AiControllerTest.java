package com.daesabu.meongcoach.ai.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailView;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportView;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlView;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AI 리포트 목록·상세 조회와 영상 업로드 URL 발급·체험 횟수 조회 API 검증.
 */
@WebMvcTest(AiController.class)
@AutoConfigureRestDocs
@DisplayName("AI API")
class AiControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다
	// (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	private static final String VIDEO_OBJECT_KEY = "videos/training/42/uuid.mp4";
	private static final String VIDEO_UPLOAD_URL =
			"https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/" + VIDEO_OBJECT_KEY
					+ "?X-Amz-Expires=900&X-Amz-Signature=example";
	private static final String VIDEO_PUBLIC_URL = "https://videos.test.meongcoach.com/" + VIDEO_OBJECT_KEY;

	private static final String VIDEO_ISSUE_REQUEST = """
			{
				"contentType": "video/mp4",
				"fileSizeBytes": 10485760
			}
			""";

	private static final AiReportContent REPORT_CONTENT = new AiReportContent(
			List.of(new AiReportContent.Recommend("분리불안 교육", "혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요.")),
			List.of(
					new AiReportContent.ReportSection("영상에서 이런 행동이 보여요", "현관 앞을 서성여요."),
					new AiReportContent.ReportSection("이런 이유로 문제 행동으로 볼 수 있어요", "분리불안 징후일 수 있어요.")),
			List.of(new AiReportContent.Solution(1, "혼자 있는 연습", "짧게 자리를 비워 보세요.")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiReportFinder aiReportFinder;

	@MockitoBean
	private AiVideoUploadUrlIssuer aiVideoUploadUrlIssuer;

	@MockitoBean
	private AiTrialFinder aiTrialFinder;

	@Test
	@DisplayName("리포트 목록을 최신순으로 반환한다")
	void findReportsReturnsReportsLatestFirst() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of(
				new AiReportView(11L, "videos/training/42/second.mp4", "물체를 물고 달리는 행동 분석",
						LocalDateTime.of(2026, 8, 2, 10, 30, 0)),
				new AiReportView(10L, "videos/training/42/first.mp4", "분리불안 징후 행동 분석",
						LocalDateTime.of(2026, 8, 1, 9, 0, 0))
		));

		mockMvc.perform(get("/api/ai/reports")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reports[0].reportId").value(11))
				.andExpect(jsonPath("$.reports[0].videoObjectKey").value("videos/training/42/second.mp4"))
				.andExpect(jsonPath("$.reports[0].title").value("물체를 물고 달리는 행동 분석"))
				.andExpect(jsonPath("$.reports[0].createdAt").value("2026-08-02T10:30:00"))
				.andExpect(jsonPath("$.reports[1].reportId").value(10))
				.andExpect(jsonPath("$.reports[1].videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.reports[1].title").value("분리불안 징후 행동 분석"))
				.andExpect(jsonPath("$.reports[1].createdAt").value("2026-08-01T09:00:00"))
				.andDo(document("ai/report-list",
						responseFields(
								fieldWithPath("reports[]").description("사용자의 AI 리포트 목록. 생성 시각 내림차순"),
								fieldWithPath("reports[].reportId").description("리포트 ID"),
								fieldWithPath("reports[].videoObjectKey").description("분석한 영상의 S3 객체 키"),
								fieldWithPath("reports[].title").optional()
										.description("AI가 요약한 리포트 제목. 생성에 실패한 리포트는 null"),
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
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", REPORT_CONTENT,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reportId").value(10))
				.andExpect(jsonPath("$.videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.title").value("분리불안 징후 행동 분석"))
				.andExpect(jsonPath("$.content.recommend[0].title").value("분리불안 교육"))
				.andExpect(jsonPath("$.content.recommend[0].description")
						.value("혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요."))
				.andExpect(jsonPath("$.content.report[0].subTitle").value("영상에서 이런 행동이 보여요"))
				.andExpect(jsonPath("$.content.report[0].description").value("현관 앞을 서성여요."))
				.andExpect(jsonPath("$.content.solution[0].order").value(1))
				.andExpect(jsonPath("$.content.solution[0].title").value("혼자 있는 연습"))
				.andExpect(jsonPath("$.createdAt").value("2026-08-01T09:00:00"))
				.andDo(document("ai/report-detail",
						pathParameters(
								parameterWithName("reportId").description("조회할 리포트 ID")
						),
						responseFields(
								fieldWithPath("reportId").description("리포트 ID"),
								fieldWithPath("videoObjectKey").description("분석한 영상의 S3 객체 키"),
								fieldWithPath("title").optional()
										.description("AI가 요약한 리포트 제목. 생성에 실패한 리포트는 null"),
								fieldWithPath("content").description("AI 분석 리포트 본문"),
								fieldWithPath("content.recommend[]").description(
										"추천 교육 목록. 문제 행동이 아닌 영상은 빈 배열"),
								fieldWithPath("content.recommend[].title").description("추천 교육 이름"),
								fieldWithPath("content.recommend[].description").optional()
										.description("이 교육을 추천하는 이유. 도입 전에 생성된 리포트는 null"),
								fieldWithPath("content.report[]").description("리포트 문단 목록. 항상 1개 이상"),
								fieldWithPath("content.report[].subTitle").description("문단 소제목"),
								fieldWithPath("content.report[].description").description("문단 내용"),
								fieldWithPath("content.solution[]").description(
										"교정 단계 목록. 문제 행동이 아닌 영상은 빈 배열"),
								fieldWithPath("content.solution[].order").description("단계 순서. 1부터 시작"),
								fieldWithPath("content.solution[].title").description("단계 제목"),
								fieldWithPath("content.solution[].description").description("단계 설명"),
								fieldWithPath("createdAt").description("리포트 생성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 리포트 상세 조회를 위임한다")
	void findReportDelegatesWithCurrentUserId() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailView(10L,
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", REPORT_CONTENT,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiReportFinder).should().findReport(42L, 10L);
	}

	@Test
	@DisplayName("없거나 본인 소유가 아닌 리포트면 404와 에러 코드를 반환한다")
	void findReportReturnsNotFoundWhenReportDoesNotExistOrIsNotOwned() throws Exception {
		given(aiReportFinder.findReport(42L, 999L)).willThrow(new AiReportNotFoundException(999L));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
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

	@Test
	@DisplayName("체험 횟수가 남아 있으면 영상 업로드 URL을 발급한다")
	void issueVideoUploadUrlReturnsUploadUrl() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, "video/mp4", 10485760L)).willReturn(
				new AiVideoUploadUrlView(VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L));

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(VIDEO_UPLOAD_URL))
				.andExpect(jsonPath("$.publicUrl").value(VIDEO_PUBLIC_URL))
				.andExpect(jsonPath("$.objectKey").value(VIDEO_OBJECT_KEY))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andDo(document("ai/video-upload-url",
						requestFields(
								fieldWithPath("contentType").description(
										"필수 입력. 업로드할 영상의 Content-Type. `video/mp4`, `video/quicktime`만 지원"),
								fieldWithPath("fileSizeBytes").description(
										"필수 입력. 업로드할 영상의 바이트 수. 1 이상 52428800(50MB) 이하여야 하며, "
												+ "이 값이 그대로 presigned URL의 Content-Length 서명에 들어간다")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`은 요청한 값과, "
												+ "`Content-Length`는 요청한 `fileSizeBytes`와 정확히 같아야 한다"),
								fieldWithPath("publicUrl").description(
										"공개 도메인 기준의 영상 URL. 버킷을 비공개로 운영하면 직접 접근은 거부된다"),
								fieldWithPath("objectKey").description(
										"업로드된 객체의 키. 이후 API 요청에는 이 값을 담아 등록한다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 영상 업로드 URL 발급을 위임한다")
	void issueVideoUploadUrlDelegatesWithCurrentUserId() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, "video/mp4", 10485760L)).willReturn(
				new AiVideoUploadUrlView(VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L));

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk());

		then(aiVideoUploadUrlIssuer).should().issue(42L, "video/mp4", 10485760L);
	}

	@Test
	@DisplayName("체험 횟수를 소진했으면 403과 에러 코드를 반환한다")
	void issueVideoUploadUrlReturnsForbiddenWhenTrialExhausted() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, "video/mp4", 10485760L))
				.willThrow(new AiReportTrialExceededException());

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("AI_REPORT_TRIAL_EXCEEDED"))
				.andDo(document("ai/video-upload-url-error",
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
	@DisplayName("영상 Content-Type이 비어 있으면 검증에 실패한다")
	void issueVideoUploadUrlFailsWhenContentTypeIsBlank() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("video/mp4", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("contentType"));
	}

	@Test
	@DisplayName("영상 파일 크기가 없으면 검증에 실패한다")
	void issueVideoUploadUrlFailsWhenFileSizeIsMissing() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));
	}

	@Test
	@DisplayName("인증 정보가 없으면 영상 업로드 URL 발급도 401을 반환한다")
	void issueVideoUploadUrlReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("체험 횟수 사용 현황을 반환한다")
	void findTrialReturnsTrialUsage() throws Exception {
		given(aiTrialFinder.findTrial(42L)).willReturn(new AiTrial(1));

		mockMvc.perform(get("/api/ai/trial")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usedCount").value(1))
				.andExpect(jsonPath("$.maxCount").value(3))
				.andExpect(jsonPath("$.remainingCount").value(2))
				.andDo(document("ai/trial",
						responseFields(
								fieldWithPath("usedCount").description("지금까지 생성한 AI 리포트 수"),
								fieldWithPath("maxCount").description("무료 체험 최대 횟수"),
								fieldWithPath("remainingCount").description("남은 횟수. 소진했으면 0")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 체험 횟수 조회를 위임한다")
	void findTrialDelegatesWithCurrentUserId() throws Exception {
		given(aiTrialFinder.findTrial(42L)).willReturn(new AiTrial(0));

		mockMvc.perform(get("/api/ai/trial").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiTrialFinder).should().findTrial(42L);
	}

	@Test
	@DisplayName("인증 정보가 없으면 체험 횟수 조회도 401을 반환한다")
	void findTrialReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/ai/trial"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
