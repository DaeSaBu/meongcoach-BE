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

import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegister;
import com.daesabu.meongcoach.ai.application.provided.AiAnalysisRegisterResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlIssuer;
import com.daesabu.meongcoach.ai.application.provided.AiVideoUploadUrlResult;
import com.daesabu.meongcoach.ai.application.provided.dto.AiAnalysisRegisterRequest;
import com.daesabu.meongcoach.ai.application.provided.dto.AiVideoUploadUrlRequest;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiTrialFixture;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.ai.domain.exception.AiVideoDurationOutOfRangeException;
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

@WebMvcTest(AiController.class)
@AutoConfigureRestDocs
class AiControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	private static final String VIDEO_OBJECT_KEY = "videos/training/42/uuid.mp4";
	private static final String VIDEO_UPLOAD_URL =
			"https://storage.googleapis.com/test-ai-video-bucket/" + VIDEO_OBJECT_KEY
					+ "?X-Goog-Expires=900&X-Goog-Signature=example";
	private static final String VIDEO_PUBLIC_URL = "https://storage.googleapis.com/test-ai-video-bucket/" + VIDEO_OBJECT_KEY;

	private static final String VIDEO_ISSUE_REQUEST = """
			{
				"contentType": "video/mp4",
				"fileSizeBytes": 10485760,
				"durationSeconds": 60
			}
			""";
	private static final AiVideoUploadUrlRequest VIDEO_REQUEST =
			new AiVideoUploadUrlRequest("video/mp4", 10485760L, 60L);

	private static final String ANALYSIS_CONTEXT = "퇴근하고 집에 오면 매번 이렇게 짖어요";
	private static final String ANALYSIS_REGISTER_REQUEST = """
			{
				"contentType": "video/mp4",
				"fileSizeBytes": 10485760,
				"durationSeconds": 60,
				"context": "퇴근하고 집에 오면 매번 이렇게 짖어요"
			}
			""";
	private static final AiAnalysisRegisterRequest ANALYSIS_REQUEST =
			new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, ANALYSIS_CONTEXT);
	private static final AiAnalysisRegisterResult ANALYSIS_RESULT =
			new AiAnalysisRegisterResult(10L, VIDEO_UPLOAD_URL, VIDEO_OBJECT_KEY, 900L);

	private static final AiReportContent REPORT_CONTENT = new AiReportContent(
			List.of(new AiReportContent.Recommend(106L, "분리불안 교육", "혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요.")),
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

	@MockitoBean
	private AiAnalysisRegister aiAnalysisRegister;

	@Test
	void 리포트_목록을_최신순으로_반환한다() throws Exception {
		given(aiReportFinder.findReports(42L)).willReturn(List.of(
				new AiReportResult(11L, "videos/training/42/second.mp4", "물체를 물고 달리는 행동 분석",
						AiAnalysisStatus.COMPLETED, LocalDateTime.of(2026, 8, 2, 10, 30, 0)),
				new AiReportResult(10L, "videos/training/42/first.mp4", "분리불안 징후 행동 분석",
						AiAnalysisStatus.COMPLETED, LocalDateTime.of(2026, 8, 1, 9, 0, 0))
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
								fieldWithPath("reports[].videoObjectKey").description("분석한 영상의 GCS 객체 키"),
								fieldWithPath("reports[].title").optional()
										.description("AI가 요약한 리포트 제목. COMPLETED가 아니거나 제목 생성에 실패한 리포트는 null"),
								fieldWithPath("reports[].status").description(
										"리포트 상태. UPLOADING(업로드 대기), ANALYZING(분석 중), COMPLETED(완료), FAILED_UPLOAD(업로드 미완료), "
												+ "FAILED_TRIAL_EXCEEDED(체험 횟수 초과), FAILED_ANALYSIS(분석 실패), FAILED_UNEXPECTED(예기치 못한 오류). "
												+ "UPLOADING·ANALYZING이 아니면 폴링을 멈춘다"),
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
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", AiAnalysisStatus.COMPLETED, REPORT_CONTENT,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reportId").value(10))
				.andExpect(jsonPath("$.videoObjectKey").value("videos/training/42/first.mp4"))
				.andExpect(jsonPath("$.title").value("분리불안 징후 행동 분석"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.content.recommend[0].topicId").value(106))
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
								fieldWithPath("videoObjectKey").description("분석한 영상의 GCS 객체 키"),
								fieldWithPath("title").optional()
										.description("AI가 요약한 리포트 제목. COMPLETED가 아니거나 제목 생성에 실패한 리포트는 null"),
								fieldWithPath("status").description(
										"리포트 상태. UPLOADING(업로드 대기), ANALYZING(분석 중), COMPLETED(완료), FAILED_UPLOAD(업로드 미완료), "
												+ "FAILED_TRIAL_EXCEEDED(체험 횟수 초과), FAILED_ANALYSIS(분석 실패), FAILED_UNEXPECTED(예기치 못한 오류). "
												+ "UPLOADING·ANALYZING이 아니면 폴링을 멈춘다"),
								fieldWithPath("content").optional().description("AI 분석 리포트 본문. COMPLETED가 아니면 null"),
								fieldWithPath("content.recommend[]").description(
										"추천 교육 목록. 문제 행동이 아닌 영상은 빈 배열"),
								fieldWithPath("content.recommend[].topicId").optional()
										.description("추천 교육의 토픽 ID. 도입 전에 생성된 리포트는 null"),
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
				"videos/training/42/first.mp4", null, AiAnalysisStatus.ANALYZING, null,
				LocalDateTime.of(2026, 8, 1, 9, 0, 0)));

		mockMvc.perform(get("/api/ai/reports/{reportId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANALYZING"))
				.andExpect(jsonPath("$.title").value(nullValue()))
				.andExpect(jsonPath("$.content").value(nullValue()));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_리포트_상세_조회를_위임한다() throws Exception {
		given(aiReportFinder.findReport(42L, 10L)).willReturn(new AiReportDetailResult(10L,
				"videos/training/42/first.mp4", "분리불안 징후 행동 분석", AiAnalysisStatus.COMPLETED, REPORT_CONTENT,
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
		given(aiVideoUploadUrlIssuer.issue(42L, VIDEO_REQUEST)).willReturn(
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
										"필수 입력. 업로드할 영상의 바이트 수. 1 이상 536870912(512MiB) 이하여야 하며, "
												+ "이 값이 그대로 presigned URL의 Content-Length 서명에 들어간다"),
								fieldWithPath("durationSeconds").description(
										"필수 입력. 업로드할 영상의 길이(초, 정수). 10 이상 300(5분) 이하여야 한다")
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
		given(aiVideoUploadUrlIssuer.issue(42L, VIDEO_REQUEST)).willReturn(
				new AiVideoUploadUrlResult(10L, VIDEO_UPLOAD_URL, VIDEO_PUBLIC_URL, VIDEO_OBJECT_KEY, 900L));

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isOk());

		then(aiVideoUploadUrlIssuer).should().issue(42L, VIDEO_REQUEST);
	}

	@Test
	void 체험_횟수를_소진했으면_403과_에러_코드를_반환한다() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, VIDEO_REQUEST))
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
						.content("{\"contentType\": \"video/mp4\", \"durationSeconds\": 60}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));
	}

	@Test
	void 영상_길이가_없으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\", \"fileSizeBytes\": 10485760}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("durationSeconds"));
	}

	@Test
	void 영상_길이가_허용_범위를_벗어나면_400과_에러_코드를_반환한다() throws Exception {
		given(aiVideoUploadUrlIssuer.issue(42L, VIDEO_REQUEST))
				.willThrow(new AiVideoDurationOutOfRangeException());

		mockMvc.perform(post("/api/ai/presigned-urls")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AI_VIDEO_DURATION_OUT_OF_RANGE"));
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
	void 맥락을_담은_분석_건을_만들고_분석_건_ID와_영상_업로드_URL을_201로_반환한다() throws Exception {
		given(aiAnalysisRegister.register(42L, ANALYSIS_REQUEST)).willReturn(ANALYSIS_RESULT);

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ANALYSIS_REGISTER_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.analysisId").value(10))
				.andExpect(jsonPath("$.uploadUrl").value(VIDEO_UPLOAD_URL))
				.andExpect(jsonPath("$.objectKey").value(VIDEO_OBJECT_KEY))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andDo(document("ai/analysis-register",
						requestFields(
								fieldWithPath("contentType").description(
										"필수 입력. 업로드할 영상(압축 후)의 Content-Type. `video/mp4`, `video/quicktime`만 지원"),
								fieldWithPath("fileSizeBytes").description(
										"필수 입력. 업로드할 영상(압축 후)의 바이트 수. 1 이상 536870912(512MiB) 이하여야 하며, "
												+ "이 값이 그대로 presigned URL의 Content-Length 서명에 들어가므로 압축이 끝난 뒤 요청한다"),
								fieldWithPath("durationSeconds").description(
										"필수 입력. 업로드할 영상의 길이(초, 정수). 10 이상 300(5분) 이하여야 한다"),
								fieldWithPath("context").optional().description(
										"선택 입력. 보호자가 남긴 영상의 맥락. 최대 500자이며 영상 분석에 참고 정보로 쓰인다")
						),
						responseFields(
								fieldWithPath("analysisId").description(
										"UPLOADING 상태로 만든 분석 건 ID. 리포트 목록·상세 조회의 `reportId`와 같은 값이다"),
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`은 요청한 값과, "
												+ "`Content-Length`는 요청한 `fileSizeBytes`와 정확히 같아야 한다"),
								fieldWithPath("objectKey").description("업로드된 객체의 키"),
								fieldWithPath("expiresInSeconds").description(
										"uploadUrl의 유효 시간(초). 이 안에 업로드를 마치지 않으면 분석 건은 FAILED_UPLOAD로 조회된다")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_분석_건_생성을_위임한다() throws Exception {
		given(aiAnalysisRegister.register(42L, ANALYSIS_REQUEST)).willReturn(ANALYSIS_RESULT);

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(ANALYSIS_REGISTER_REQUEST))
				.andExpect(status().isCreated());

		then(aiAnalysisRegister).should().register(42L, ANALYSIS_REQUEST);
	}

	@Test
	void 맥락이_없어도_분석_건을_만든다() throws Exception {
		AiAnalysisRegisterRequest request = new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, null);
		given(aiAnalysisRegister.register(42L, request)).willReturn(ANALYSIS_RESULT);

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VIDEO_ISSUE_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.analysisId").value(10));
	}

	@Test
	void 맥락_길이는_앞뒤_공백을_지운_뒤_센다() throws Exception {
		String context = "가".repeat(500);
		String request = ANALYSIS_REGISTER_REQUEST.replace(ANALYSIS_CONTEXT, "  " + context + "\\n");
		given(aiAnalysisRegister.register(42L, new AiAnalysisRegisterRequest("video/mp4", 10485760L, 60L, context)))
				.willReturn(ANALYSIS_RESULT);

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.analysisId").value(10));
	}

	@Test
	void 맥락이_500자를_넘으면_검증에_실패한다() throws Exception {
		String request = ANALYSIS_REGISTER_REQUEST.replace(ANALYSIS_CONTEXT, "가".repeat(501));

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("context"));
	}

	@Test
	void 체험_횟수를_소진했으면_분석_건_생성도_403과_에러_코드를_반환한다() throws Exception {
		given(aiAnalysisRegister.register(42L, ANALYSIS_REQUEST))
				.willThrow(new AiReportTrialExceededException());

		mockMvc.perform(post("/api/ai/analyses")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ANALYSIS_REGISTER_REQUEST))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AI_REPORT_TRIAL_EXCEEDED"))
				.andDo(document("ai/analysis-register-error",
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
	void 인증_정보가_없으면_분석_건_생성도_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/ai/analyses")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ANALYSIS_REGISTER_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 체험_횟수_사용_현황을_반환한다() throws Exception {
		given(aiTrialFinder.findTrial(42L)).willReturn(AiTrialFixture.used(42L, 1));

		mockMvc.perform(get("/api/ai/trial")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usedCount").value(1))
				.andExpect(jsonPath("$.maxCount").value(30))
				.andExpect(jsonPath("$.remainingCount").value(29))
				.andDo(document("ai/trial",
						responseFields(
								fieldWithPath("usedCount").description("사용한 횟수. 분석이 완료될 때 1씩 늘고 실패·진행 중은 세지 않는다"),
								fieldWithPath("maxCount").description("이 사용자에게 부여된 무료 체험 횟수. 기본 30"),
								fieldWithPath("remainingCount").description("남은 횟수. 소진했으면 0")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_체험_횟수_조회를_위임한다() throws Exception {
		given(aiTrialFinder.findTrial(42L)).willReturn(AiTrial.start(42L));

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
