package com.daesabu.meongcoach.graduation.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionFixture;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionStatus;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraduationController.class)
@AutoConfigureRestDocs
class GraduationControllerTest {

	private static final Principal CURRENT_USER = () -> "42";
	private static final Long TOPIC_ID = 7L;
	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-14T05:03:00Z");
	private static final Instant REVIEWED_AT = Instant.parse("2026-10-15T01:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/7/550e8400.mp4";
	private static final String VIDEO_URL =
			"https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/" + VIDEO_KEY + "?X-Amz-Signature=example";
	private static final TopicResult TOPIC = new TopicResult(TOPIC_ID, "분리불안", "혼자 있는 시간을 편안하게");

	private static final FieldDescriptor[] ERROR_FIELDS = {
			fieldWithPath("title").description("HTTP 상태 이름"),
			fieldWithPath("status").description("HTTP 상태 코드"),
			fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
			fieldWithPath("instance").description("에러가 발생한 요청 경로"),
			fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
			fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
	};

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GraduationFinder graduationFinder;

	@MockitoBean
	private GraduationUploadUrlIssuer graduationUploadUrlIssuer;

	@MockitoBean
	private GraduationSubmitter graduationSubmitter;

	@Test
	void 토픽의_실습_인증_현황을_반환한다() throws Exception {
		GraduationSubmission graduation = GraduationSubmissionFixture.create(3L, 42L, TOPIC_ID, GraduationSubmissionStatus.APPROVED,
				VIDEO_KEY, SUBMITTED_AT, REVIEWED_AT, "끝까지 함께 시도했어요!");
		given(graduationFinder.findGraduation(42L, TOPIC_ID))
				.willReturn(new GraduationDetailResult(TOPIC, true, graduation, VIDEO_URL));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(7))
				.andExpect(jsonPath("$.topicTitle").value("분리불안"))
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.graduation.submissionId").value(3))
				.andExpect(jsonPath("$.graduation.status").value("APPROVED"))
				.andExpect(jsonPath("$.graduation.attemptNo").doesNotExist())
				.andExpect(jsonPath("$.graduation.videoUrl").value(VIDEO_URL))
				.andExpect(jsonPath("$.graduation.submittedAt").value("2026-10-14T05:03:00Z"))
				.andExpect(jsonPath("$.graduation.reviewedAt").value("2026-10-15T01:00:00Z"))
				.andExpect(jsonPath("$.graduation.review.note").value("끝까지 함께 시도했어요!"))
				.andExpect(jsonPath("$.graduation.review.retakeInstructions").isEmpty())
				.andExpect(jsonPath("$.graduation.retakeImageUrl").isEmpty())
				.andDo(document("graduation/detail",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						responseFields(
								fieldWithPath("topicId").description("토픽 ID"),
								fieldWithPath("topicTitle").description("토픽 제목"),
								fieldWithPath("eligible").description("토픽의 모든 커리큘럼을 완료해 실습 인증을 할 수 있으면 true"),
								fieldWithPath("graduation").optional().description("토픽 수료 현황. 제출한 적이 없으면 null"),
								fieldWithPath("graduation.submissionId").description("토픽 수료 ID"),
								fieldWithPath("graduation.status").description(
										"수료 상태. SUBMITTED(검수 대기), REVISION_REQUESTED(보완 요청), APPROVED(수료)"),
								fieldWithPath("graduation.videoUrl").description("가장 최근 제출 영상의 1시간 유효 presigned URL"),
								fieldWithPath("graduation.submittedAt").description("가장 최근 제출 시각(ISO-8601, UTC)"),
								fieldWithPath("graduation.reviewedAt").optional().description("가장 최근 제출의 판정 시각. 판정 전이면 null"),
								fieldWithPath("graduation.review.note").optional().description("운영자 메모. 없으면 null"),
								fieldWithPath("graduation.review.retakeInstructions").description("보완 요청 때 운영자가 남긴 재촬영 팁(최대 3개, 순서대로). 없으면 빈 배열"),
								fieldWithPath("graduation.retakeImageUrl").optional().description(
										"보완 요청 때 운영자가 올린 촬영 예시 이미지의 공개 URL. 없으면 null이고, 앱은 기본 그림을 보여 준다")
						)
				));
	}

	@Test
	void 보완_요청이면_재촬영_팁과_촬영_예시_이미지를_함께_반환한다() throws Exception {
		GraduationSubmission graduation = GraduationSubmissionFixture.create(3L, 42L, TOPIC_ID,
				GraduationSubmissionStatus.REVISION_REQUESTED, VIDEO_KEY, SUBMITTED_AT, REVIEWED_AT, "현관 장면부터 다시 찍어 주세요.");
		GraduationSubmission submission = GraduationSubmissionFixture.withRetakeGuide(
				GraduationSubmissionFixture.submission(graduation, REVIEWED_AT, "현관 장면부터 다시 찍어 주세요."),
				"https://images.test/images/graduation-retake/3/example.jpg",
				List.of("현관이 함께 보이게 찍어 주세요.", "보호자가 나가는 순간부터 찍어 주세요."));
		given(graduationFinder.findGraduation(42L, TOPIC_ID))
				.willReturn(new GraduationDetailResult(TOPIC, true, submission, VIDEO_URL));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduation.status").value("REVISION_REQUESTED"))
				.andExpect(jsonPath("$.graduation.review.retakeInstructions[0]").value("현관이 함께 보이게 찍어 주세요."))
				.andExpect(jsonPath("$.graduation.review.retakeInstructions[1]").value("보호자가 나가는 순간부터 찍어 주세요."))
				.andExpect(jsonPath("$.graduation.retakeImageUrl").value("https://images.test/images/graduation-retake/3/example.jpg"));
	}

	@Test
	void 내_훈련_영상_목록을_최근_제출_순으로_반환한다() throws Exception {
		GraduationSubmission graduation = GraduationSubmissionFixture.create(3L, 42L, TOPIC_ID, GraduationSubmissionStatus.APPROVED,
				VIDEO_KEY, SUBMITTED_AT, REVIEWED_AT, "끝까지 함께 시도했어요!");
		given(graduationFinder.findGraduations(42L)).willReturn(List.of(new GraduationSummaryResult(
				GraduationSubmissionFixture.submission(graduation, REVIEWED_AT, "끝까지 함께 시도했어요!"), TOPIC)));

		mockMvc.perform(get("/api/training/graduations")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduations[0].submissionId").value(3))
				.andExpect(jsonPath("$.graduations[0].topicId").value(7))
				.andExpect(jsonPath("$.graduations[0].topicTitle").value("분리불안"))
				.andExpect(jsonPath("$.graduations[0].status").value("APPROVED"))
				.andExpect(jsonPath("$.graduations[0].attemptNo").doesNotExist())
				.andExpect(jsonPath("$.graduations[0].submittedAt").value("2026-10-14T05:03:00Z"))
				.andExpect(jsonPath("$.graduations[0].reviewedAt").value("2026-10-15T01:00:00Z"))
				.andExpect(jsonPath("$.graduations[0].review.note").value("끝까지 함께 시도했어요!"))
				.andDo(document("graduation/list",
						responseFields(
								fieldWithPath("graduations[]").description("제출한 적이 있는 토픽의 실습 인증. 가장 최근 제출 순"),
								fieldWithPath("graduations[].submissionId").description("토픽 수료 ID"),
								fieldWithPath("graduations[].topicId").description("토픽 ID"),
								fieldWithPath("graduations[].topicTitle").description("토픽 제목"),
								fieldWithPath("graduations[].status").description(
										"수료 상태. SUBMITTED(검수 대기), REVISION_REQUESTED(보완 요청), APPROVED(수료)"),
								fieldWithPath("graduations[].submittedAt").description("가장 최근 제출 시각(ISO-8601, UTC)"),
								fieldWithPath("graduations[].reviewedAt").optional().description("가장 최근 제출의 판정 시각. 판정 전이면 null"),
								fieldWithPath("graduations[].review.note").optional().description("운영자 메모. 없으면 null"),
 fieldWithPath("graduations[].review.retakeInstructions").description("재촬영 지시")
						)
				));
	}

	@Test
	void 제출한_적이_없으면_graduation을_null로_반환한다() throws Exception {
		given(graduationFinder.findGraduation(42L, TOPIC_ID)).willReturn(GraduationDetailResult.notSubmitted(TOPIC, false));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.graduation").isEmpty());
	}

	@Test
	void 없는_토픽이면_404와_에러_코드를_반환한다() throws Exception {
		given(graduationFinder.findGraduation(42L, 999L)).willThrow(new TopicNotFoundException(999L));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"))
				.andDo(document("graduation/detail-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 실습_영상_업로드_URL을_발급하고_201을_반환한다() throws Exception {
		given(graduationUploadUrlIssuer.issue(42L, TOPIC_ID, new GraduationUploadUrlRequest("video/mp4", 10485760L)))
				.willReturn(new VideoUploadUrlResult(VIDEO_URL, "https://videos.test/" + VIDEO_KEY, VIDEO_KEY, 900L));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/presigned-urls", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\", \"fileSizeBytes\": 10485760}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.uploadUrl").value(VIDEO_URL))
				.andExpect(jsonPath("$.objectKey").value(VIDEO_KEY))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andExpect(jsonPath("$.publicUrl").doesNotExist())
				.andDo(document("graduation/upload-url",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						requestFields(
								fieldWithPath("contentType").description("필수 입력. `video/mp4`, `video/quicktime`만 지원"),
								fieldWithPath("fileSizeBytes").description(
										"필수 입력. 영상 바이트 수. 1 이상 536870912바이트(512MiB) 이하. presigned URL의 Content-Length 서명에 들어간다")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`·`Content-Length`가 요청 값과 같아야 한다"),
								fieldWithPath("objectKey").description("업로드한 뒤 제출에 그대로 담을 객체 키"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	void 영상이_상한을_넘으면_공통_에러_응답으로_발급을_거절한다() throws Exception {
		given(graduationUploadUrlIssuer.issue(42L, TOPIC_ID, new GraduationUploadUrlRequest("video/mp4", 536870913L)))
				.willThrow(new VideoFileSizeExceededException(536870913L, 536870912L));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/presigned-urls", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\", \"fileSizeBytes\": 536870913}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_VIDEO_FILE_SIZE_EXCEEDED"))
				.andExpect(jsonPath("$.maxFileSizeBytes").doesNotExist())
				.andExpect(jsonPath("$.uploadUrl").doesNotExist())
				.andDo(document("graduation/upload-url-size-error",
						responseFields(ERROR_FIELDS)));
	}

	@Test
	void 커리큘럼을_다_마치지_않았으면_업로드_URL_발급에_403을_반환한다() throws Exception {
		given(graduationUploadUrlIssuer.issue(42L, TOPIC_ID, new GraduationUploadUrlRequest("video/mp4", 10485760L)))
				.willThrow(new TopicNotCompletedException(TOPIC_ID));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/presigned-urls", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contentType\": \"video/mp4\", \"fileSizeBytes\": 10485760}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("GRADUATION_TOPIC_NOT_COMPLETED"))
				.andDo(document("graduation/upload-url-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 업로드한_영상으로_제출하고_201을_반환한다() throws Exception {
		given(graduationSubmitter.submit(42L, TOPIC_ID, new GraduationSubmissionRequest(VIDEO_KEY)))
				.willReturn(GraduationSubmissionFixture.create(3L, 42L, TOPIC_ID, GraduationSubmissionStatus.SUBMITTED, VIDEO_KEY,
						SUBMITTED_AT, null, null));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/submissions", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"objectKey\": \"" + VIDEO_KEY + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.submissionId").value(3))
				.andExpect(jsonPath("$.attemptNo").doesNotExist())
				.andExpect(jsonPath("$.submittedAt").value("2026-10-14T05:03:00Z"))
				.andDo(document("graduation/submission",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						requestFields(fieldWithPath("objectKey").description(
								"필수 입력. 업로드 URL 발급 응답의 objectKey. 본인이 이 토픽으로 받은 `videos/graduation/{userId}/{topicId}/` 키만 받는다")),
						responseFields(
								fieldWithPath("submissionId").description("토픽 수료 ID. 운영자 판정에 쓴다"),
								fieldWithPath("submittedAt").description("제출 시각(ISO-8601, UTC)")
						)
				));
	}

	@Test
	void 이미_수료한_토픽에_제출하면_409를_반환한다() throws Exception {
		given(graduationSubmitter.submit(42L, TOPIC_ID, new GraduationSubmissionRequest(VIDEO_KEY)))
				.willThrow(new GraduationAlreadyApprovedException(TOPIC_ID));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/submissions", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"objectKey\": \"" + VIDEO_KEY + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("GRADUATION_ALREADY_APPROVED"))
				.andDo(document("graduation/submission-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 객체_키가_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/submissions", TOPIC_ID)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"objectKey\": \" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/training/graduations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
