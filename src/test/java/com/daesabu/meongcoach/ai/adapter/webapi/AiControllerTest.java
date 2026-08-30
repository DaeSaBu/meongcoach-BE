package com.daesabu.meongcoach.ai.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
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
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
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
	void 리포트_목록을_최신순으로_반환한다() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of(
				new AiReportResult(11L, "videos/training/42/second.mp4", "물체를 물고 달리는 행동 분석",
						AiReportStatus.COMPLETED, LocalDateTime.of(2026, 8, 2, 10, 30, 0)),
				new AiReportResult(10L, "videos/training/42/first.mp4", "분리불안 징후 행동 분석",
						AiReportStatus.COMPLETED, LocalDateTime.of(2026, 8, 1, 9, 0, 0))
		));

		mockMvc.perform(get("/api/ai/reports")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reports[0].reportId").value(11))
				.andExpect(jsonPath("$.reports[0].videoObjectKey").value("videos/training/42/second.mp4"))
				.andExpect(jsonPath("$.reports[0].title").value("물체를 물고 달리는 행동 분석"))
				.andExpect(jsonPath("$.reports[0].status").value("COMPLETED"))
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
										.description("AI가 요약한 리포트 제목. COMPLETED가 아니거나 제목 생성에 실패한 리포트는 null"),
								fieldWithPath("reports[].status").description(
										"리포트 상태. UPLOADING(업로드 대기), PENDING(분석 중), COMPLETED(완료), FAILED_UPLOAD(업로드 미완료), "
												+ "FAILED_TRIAL_EXCEEDED(체험 횟수 초과), FAILED_ANALYSIS(분석 실패), FAILED_UNEXPECTED(예기치 못한 오류). "
												+ "UPLOADING·PENDING이 아니면 폴링을 멈춘다"),
								fieldWithPath("reports[].createdAt").description("리포트 생성 시각(ISO-8601, 초 단위)")
						)
				));
	}

	@Test
	void 리포트가_없으면_빈_배열과_200을_반환한다() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/ai/reports").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reports").isArray())
				.andExpect(jsonPath("$.reports").isEmpty());
	}

	@Test
	void 인증_주체에서_읽은_사용자로_리포트_목록_조회를_위임한다() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/ai/reports").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiReportFinder).should().findReports(42L);
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/ai/reports"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 리포트_하나를_본문과_함께_반환한다() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailResult(10L,
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", AiReportStatus.COMPLETED, REPORT_CONTENT,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reportId").value(10))
				.andExpect(jsonPath("$.videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.title").value("분리불안 징후 행동 분석"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
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
										.description("AI가 요약한 리포트 제목. COMPLETED가 아니거나 제목 생성에 실패한 리포트는 null"),
								fieldWithPath("status").description(
										"리포트 상태. UPLOADING(업로드 대기), PENDING(분석 중), COMPLETED(완료), FAILED_UPLOAD(업로드 미완료), "
												+ "FAILED_TRIAL_EXCEEDED(체험 횟수 초과), FAILED_ANALYSIS(분석 실패), FAILED_UNEXPECTED(예기치 못한 오류). "
												+ "UPLOADING·PENDING이 아니면 폴링을 멈춘다"),
								fieldWithPath("content").optional().description("AI 분석 리포트 본문. COMPLETED가 아니면 null"),
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
	void 완료되지_않은_리포트_상세는_상태와_함께_본문을_null로_내린다() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailResult(10L,
				"videos/training/42/first.mp4", null, AiReportStatus.PENDING, null,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.title").value(nullValue()))
				.andExpect(jsonPath("$.content").value(nullValue()));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_리포트_상세_조회를_위임한다() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailResult(10L,
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", AiReportStatus.COMPLETED, REPORT_CONTENT,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiReportFinder).should().findReport(42L, 10L);
	}

	@Test
	void 없거나_본인_소유가_아닌_리포트면_404와_에러_코드를_반환한다() throws Exception {
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
	void 체험_횟수가_남아_있으면_영상_업로드_URL을_발급한다() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, "video/mp4", 10485760L)).willReturn(
				new AiVideoUploadUrlResult(10L, VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L));

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reportId").value(10))
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
								fieldWithPath("reportId").description(
										"발급과 함께 UPLOADING 상태로 만든 리포트 ID. 리포트 목록·상세 조회에 그대로 쓴다"),
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`은 요청한 값과, "
												+ "`Content-Length`는 요청한 `fileSizeBytes`와 정확히 같아야 한다"),
								fieldWithPath("publicUrl").description(
										"공개 도메인 기준의 영상 URL. 버킷을 비공개로 운영하면 직접 접근은 거부된다"),
								fieldWithPath("objectKey").description("업로드된 객체의 키"),
								fieldWithPath("expiresInSeconds").description(
										"uploadUrl의 유효 시간(초). 이 안에 업로드를 마치지 않으면 리포트는 FAILED_UPLOAD로 조회된다")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_영상_업로드_URL_발급을_위임한다() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, "video/mp4", 10485760L)).willReturn(
				new AiVideoUploadUrlResult(10L, VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L));

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk());

		then(aiVideoUploadUrlIssuer).should().issue(42L, "video/mp4", 10485760L);
	}

	@Test
	void 체험_횟수를_소진했으면_403과_에러_코드를_반환한다() throws Exception {
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
	void 영상_contentType이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST.replace("video/mp4", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("contentType"));
	}

	@Test
	void 영상_파일_크기가_없으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));
	}

	@Test
	void 인증_정보가_없으면_영상_업로드_URL_발급도_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 체험_횟수_사용_현황을_반환한다() throws Exception {
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
								fieldWithPath("usedCount").description("분석이 완료된 AI 리포트 수. 실패·진행 중은 세지 않는다"),
								fieldWithPath("maxCount").description("무료 체험 최대 횟수"),
								fieldWithPath("remainingCount").description("남은 횟수. 소진했으면 0")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_체험_횟수_조회를_위임한다() throws Exception {
		given(aiTrialFinder.findTrial(42L)).willReturn(new AiTrial(0));

		mockMvc.perform(get("/api/ai/trial").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(aiTrialFinder).should().findTrial(42L);
	}

	@Test
	void 인증_정보가_없으면_체험_횟수_조회도_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/ai/trial"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
