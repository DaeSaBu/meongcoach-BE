package com.daesabu.meongcoach.graduation.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmissionFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionFixture;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduationFixture;
import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import com.daesabu.meongcoach.shared.security.AdminKeyInterceptor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraduationAdminController.class)
@AutoConfigureRestDocs
class GraduationAdminControllerTest {

	private static final String ADMIN_KEY = "test-admin-key";
	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-14T05:03:00Z");
	private static final Instant REVIEWED_AT = Instant.parse("2026-10-15T01:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/550e8400.mp4";
	private static final String VIDEO_URL =
			"https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/" + VIDEO_KEY + "?X-Amz-Signature=example";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GraduationSubmissionFinder graduationSubmissionFinder;

	@MockitoBean
	private GraduationReviewer graduationReviewer;

	@Test
	void 검수_대기_제출을_오래된_순으로_반환한다() throws Exception {
		given(graduationSubmissionFinder.findSubmissions("PENDING")).willReturn(List.of(new AdminSubmissionResult(
				GraduationSubmissionFixture.create(31L, 3L, 2, VIDEO_KEY, SubmissionResult.PENDING, SUBMITTED_AT, null, null),
				TopicGraduationFixture.create(3L, 42L, 7L, GraduationStatus.SUBMITTED, null, null, null),
				"분리불안", VIDEO_URL)));

		mockMvc.perform(get("/api/admin/graduations/submissions")
						.queryParam("result", "PENDING")
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.submissions[0].submissionId").value(31))
				.andExpect(jsonPath("$.submissions[0].graduationId").value(3))
				.andExpect(jsonPath("$.submissions[0].userId").value(42))
				.andExpect(jsonPath("$.submissions[0].topicId").value(7))
				.andExpect(jsonPath("$.submissions[0].topicTitle").value("분리불안"))
				.andExpect(jsonPath("$.submissions[0].attemptNo").value(2))
				.andExpect(jsonPath("$.submissions[0].result").value("PENDING"))
				.andExpect(jsonPath("$.submissions[0].submittedAt").value("2026-10-14T05:03:00Z"))
				.andExpect(jsonPath("$.submissions[0].videoUrl").value(VIDEO_URL))
				.andDo(document("graduation/admin-submission-list",
						requestHeaders(headerWithName(AdminKeyInterceptor.HEADER).description("운영자 키(ADMIN_API_KEY)")),
						queryParameters(parameterWithName("result").optional().description(
								"조회할 제출 결과. PENDING(기본값), APPROVED, REVISION_REQUESTED, REPLACED")),
						responseFields(
								fieldWithPath("submissions[]").description("제출 목록. 오래된 제출 순"),
								fieldWithPath("submissions[].submissionId").description("제출 ID. 판정에 쓴다"),
								fieldWithPath("submissions[].graduationId").description("토픽 수료 ID"),
								fieldWithPath("submissions[].userId").description("제출한 회원 ID"),
								fieldWithPath("submissions[].topicId").description("토픽 ID"),
								fieldWithPath("submissions[].topicTitle").description("토픽 제목"),
								fieldWithPath("submissions[].attemptNo").description("몇 번째 제출인지"),
								fieldWithPath("submissions[].result").description("제출 결과"),
								fieldWithPath("submissions[].submittedAt").description("제출 시각(ISO-8601, UTC)"),
								fieldWithPath("submissions[].videoUrl").description("영상을 볼 수 있는 1시간 유효 presigned URL")
						)
				));
	}

	@Test
	void 결과를_생략하면_검수_대기_제출을_조회한다() throws Exception {
		given(graduationSubmissionFinder.findSubmissions("PENDING")).willReturn(List.of());

		mockMvc.perform(get("/api/admin/graduations/submissions").header(AdminKeyInterceptor.HEADER, ADMIN_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.submissions").isEmpty());

		then(graduationSubmissionFinder).should().findSubmissions("PENDING");
	}

	@Test
	void 제출을_판정하고_결과를_반환한다() throws Exception {
		given(graduationReviewer.review(31L, new GraduationReviewRequest("APPROVED", "끝까지 함께 시도했어요!")))
				.willReturn(GraduationSubmissionFixture.create(31L, 3L, 2, VIDEO_KEY, SubmissionResult.APPROVED,
						SUBMITTED_AT, REVIEWED_AT, "끝까지 함께 시도했어요!"));

		mockMvc.perform(put("/api/admin/graduations/submissions/{submissionId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"APPROVED\", \"note\": \"끝까지 함께 시도했어요!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.submissionId").value(31))
				.andExpect(jsonPath("$.result").value("APPROVED"))
				.andExpect(jsonPath("$.reviewedAt").value("2026-10-15T01:00:00Z"))
				.andExpect(jsonPath("$.reviewerNote").value("끝까지 함께 시도했어요!"))
				.andDo(document("graduation/admin-review",
						requestHeaders(headerWithName(AdminKeyInterceptor.HEADER).description("운영자 키(ADMIN_API_KEY)")),
						pathParameters(parameterWithName("submissionId").description("판정할 제출 ID")),
						requestFields(
								fieldWithPath("result").description("필수 입력. APPROVED(승인) 또는 REVISION_REQUESTED(보완 요청)"),
								fieldWithPath("note").optional().description(
										"운영자 메모(최대 500자). 앱에 그대로 보인다. 보완 요청에는 필수, 승인에는 선택")
						),
						responseFields(
								fieldWithPath("submissionId").description("제출 ID"),
								fieldWithPath("result").description("판정 결과"),
								fieldWithPath("reviewedAt").description("판정 시각(ISO-8601, UTC)"),
								fieldWithPath("reviewerNote").optional().description("저장한 운영자 메모. 없으면 null")
						)
				));
	}

	@Test
	void 보완_요청에_메모가_없으면_400을_반환한다() throws Exception {
		given(graduationReviewer.review(31L, new GraduationReviewRequest("REVISION_REQUESTED", null)))
				.willThrow(new ReviewNoteRequiredException());

		mockMvc.perform(put("/api/admin/graduations/submissions/{submissionId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"REVISION_REQUESTED\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("GRADUATION_REVIEW_NOTE_REQUIRED"))
				.andDo(document("graduation/admin-review-error",
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
	void 메모가_500자를_넘으면_검증에_실패한다() throws Exception {
		String longNote = "가".repeat(501);

		mockMvc.perform(put("/api/admin/graduations/submissions/{submissionId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"REVISION_REQUESTED\", \"note\": \"" + longNote + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 운영자_키가_없으면_401을_반환하고_조회하지_않는다() throws Exception {
		mockMvc.perform(get("/api/admin/graduations/submissions"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(graduationSubmissionFinder).should(never()).findSubmissions(ArgumentMatchers.any());
	}

	@Test
	void 운영자_키가_다르면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/admin/graduations/submissions").header(AdminKeyInterceptor.HEADER, "wrong-key"))
				.andExpect(status().isUnauthorized());
	}
}
