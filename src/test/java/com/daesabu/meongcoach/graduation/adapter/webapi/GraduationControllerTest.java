package com.daesabu.meongcoach.graduation.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.GraduationRewardClaimer;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionFixture;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.TopicGraduationFixture;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
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
	private static final Instant CLAIMED_AT = Instant.parse("2026-10-15T09:00:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T09:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/550e8400.mp4";
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

	@MockitoBean
	private GraduationRewardClaimer graduationRewardClaimer;

	@Test
	void 내_실습_인증_목록을_최근_제출_순으로_반환한다() throws Exception {
		TopicGraduation approved = TopicGraduationFixture.create(3L, 42L, TOPIC_ID, GraduationStatus.APPROVED,
				REVIEWED_AT, CLAIMED_AT, EXPIRES_AT);
		GraduationSubmission latest = GraduationSubmissionFixture.create(31L, 3L, 2, VIDEO_KEY,
				SubmissionResult.APPROVED, SUBMITTED_AT, REVIEWED_AT, "끝까지 함께 시도했어요!");
		given(graduationFinder.findGraduations(42L)).willReturn(List.of(new GraduationSummaryResult(approved, "분리불안", latest)));

		mockMvc.perform(get("/api/training/graduations")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduations[0].graduationId").value(3))
				.andExpect(jsonPath("$.graduations[0].topicId").value(7))
				.andExpect(jsonPath("$.graduations[0].topicTitle").value("분리불안"))
				.andExpect(jsonPath("$.graduations[0].status").value("APPROVED"))
				.andExpect(jsonPath("$.graduations[0].attemptCount").value(2))
				.andExpect(jsonPath("$.graduations[0].submittedAt").value("2026-10-14T05:03:00Z"))
				.andExpect(jsonPath("$.graduations[0].reviewerNote").value("끝까지 함께 시도했어요!"))
				.andExpect(jsonPath("$.graduations[0].reward.days").value(3))
				.andExpect(jsonPath("$.graduations[0].reward.expiresAt").value("2026-10-18T09:00:00Z"))
				.andDo(document("graduation/list",
						responseFields(
								fieldWithPath("graduations[]").description("제출한 적이 있는 토픽의 실습 인증. 가장 최근 제출 순"),
								fieldWithPath("graduations[].graduationId").description("토픽 수료 ID"),
								fieldWithPath("graduations[].topicId").description("토픽 ID"),
								fieldWithPath("graduations[].topicTitle").description("토픽 제목"),
								fieldWithPath("graduations[].status").description(
										"수료 상태. SUBMITTED(검수 대기), REVISION_REQUESTED(보완 요청), APPROVED(수료)"),
								fieldWithPath("graduations[].attemptCount").description("지금까지 제출한 횟수(영상 교체 포함)"),
								fieldWithPath("graduations[].submittedAt").description("가장 최근 제출 시각(ISO-8601, UTC)"),
								fieldWithPath("graduations[].reviewedAt").optional().description("가장 최근 제출의 판정 시각. 판정 전이면 null"),
								fieldWithPath("graduations[].reviewerNote").optional().description("운영자 메모. 없으면 null"),
								fieldWithPath("graduations[].reward.days").description("보상 일수"),
								fieldWithPath("graduations[].reward.claimedAt").optional().description("보상을 받은 시각. 받기 전이면 null"),
								fieldWithPath("graduations[].reward.expiresAt").optional().description("보상 이용권 만료 시각. 받기 전이면 null")
						)
				));
	}

	@Test
	void 토픽의_실습_인증_현황을_최근_제출과_함께_반환한다() throws Exception {
		TopicGraduation graduation = TopicGraduationFixture.create(3L, 42L, TOPIC_ID, GraduationStatus.REVISION_REQUESTED,
				null, null, null);
		GraduationSubmission latest = GraduationSubmissionFixture.create(31L, 3L, 1, VIDEO_KEY,
				SubmissionResult.REVISION_REQUESTED, SUBMITTED_AT, REVIEWED_AT, "현관 장면부터 다시 찍어 주세요.");
		given(graduationFinder.findGraduation(42L, TOPIC_ID))
				.willReturn(new GraduationDetailResult(TOPIC, true, graduation, latest, VIDEO_URL));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(7))
				.andExpect(jsonPath("$.topicTitle").value("분리불안"))
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.status").value("REVISION_REQUESTED"))
				.andExpect(jsonPath("$.graduationId").value(3))
				.andExpect(jsonPath("$.attemptCount").value(1))
				.andExpect(jsonPath("$.latestSubmission.submissionId").value(31))
				.andExpect(jsonPath("$.latestSubmission.result").value("REVISION_REQUESTED"))
				.andExpect(jsonPath("$.latestSubmission.reviewerNote").value("현관 장면부터 다시 찍어 주세요."))
				.andExpect(jsonPath("$.latestSubmission.videoUrl").value(VIDEO_URL))
				.andExpect(jsonPath("$.reward.days").value(3))
				.andExpect(jsonPath("$.reward.claimedAt").isEmpty())
				.andDo(document("graduation/detail",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						responseFields(
								fieldWithPath("topicId").description("토픽 ID"),
								fieldWithPath("topicTitle").description("토픽 제목"),
								fieldWithPath("eligible").description("토픽의 모든 커리큘럼을 완료해 실습 인증을 할 수 있으면 true"),
								fieldWithPath("status").description(
										"수료 상태. NOT_SUBMITTED(제출 전), SUBMITTED(검수 대기), REVISION_REQUESTED(보완 요청), APPROVED(수료)"),
								fieldWithPath("graduationId").optional().description("토픽 수료 ID. 제출 전이면 null"),
								fieldWithPath("attemptCount").description("지금까지 제출한 횟수. 제출 전이면 0"),
								fieldWithPath("latestSubmission").optional().description("가장 최근 제출. 제출 전이면 null"),
								fieldWithPath("latestSubmission.submissionId").description("제출 ID"),
								fieldWithPath("latestSubmission.attemptNo").description("몇 번째 제출인지(1부터)"),
								fieldWithPath("latestSubmission.result").description(
										"제출 결과. PENDING(검수 대기), APPROVED(승인), REVISION_REQUESTED(보완 요청)"),
								fieldWithPath("latestSubmission.submittedAt").description("제출 시각(ISO-8601, UTC)"),
								fieldWithPath("latestSubmission.reviewedAt").optional().description("판정 시각. 판정 전이면 null"),
								fieldWithPath("latestSubmission.reviewerNote").optional().description("운영자 메모. 없으면 null"),
								fieldWithPath("latestSubmission.videoUrl").description("제출 영상을 볼 수 있는 1시간 유효 presigned URL"),
								fieldWithPath("reward.days").description("보상 일수"),
								fieldWithPath("reward.claimedAt").optional().description("보상을 받은 시각. 받기 전이면 null"),
								fieldWithPath("reward.expiresAt").optional().description("보상 이용권 만료 시각. 받기 전이면 null")
						)
				));
	}

	@Test
	void 제출한_적이_없으면_NOT_SUBMITTED와_빈_제출로_반환한다() throws Exception {
		given(graduationFinder.findGraduation(42L, TOPIC_ID)).willReturn(GraduationDetailResult.notSubmitted(TOPIC, false));

		mockMvc.perform(get("/api/training/topics/{topicId}/graduation", TOPIC_ID).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NOT_SUBMITTED"))
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.graduationId").isEmpty())
				.andExpect(jsonPath("$.attemptCount").value(0))
				.andExpect(jsonPath("$.latestSubmission").isEmpty())
				.andExpect(jsonPath("$.reward.days").value(3));
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
										"필수 입력. 영상 바이트 수. 1 이상 52428800(50MB) 이하. presigned URL의 Content-Length 서명에 들어간다")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"영상을 PUT할 presigned URL. `Content-Type`·`Content-Length`가 요청 값과 같아야 한다"),
								fieldWithPath("objectKey").description("업로드한 뒤 제출 생성에 그대로 담을 객체 키"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
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
	void 업로드한_영상으로_제출을_만들고_201과_Location을_반환한다() throws Exception {
		given(graduationSubmitter.submit(42L, TOPIC_ID, new GraduationSubmissionRequest(VIDEO_KEY)))
				.willReturn(GraduationSubmissionFixture.create(31L, 3L, 2, VIDEO_KEY, SubmissionResult.PENDING,
						SUBMITTED_AT, null, null));

		mockMvc.perform(post("/api/training/topics/{topicId}/graduation/submissions", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"objectKey\": \"" + VIDEO_KEY + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, "/api/training/topics/7/graduation/submissions/31"))
				.andExpect(jsonPath("$.submissionId").value(31))
				.andExpect(jsonPath("$.attemptNo").value(2))
				.andExpect(jsonPath("$.result").value("PENDING"))
				.andExpect(jsonPath("$.submittedAt").value("2026-10-14T05:03:00Z"))
				.andDo(document("graduation/submission",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						requestFields(fieldWithPath("objectKey").description(
								"필수 입력. 업로드 URL 발급 응답의 objectKey. 본인의 `videos/graduation/{userId}/` 키만 받는다")),
						responseHeaders(headerWithName(HttpHeaders.LOCATION).description("만든 제출의 경로")),
						responseFields(
								fieldWithPath("submissionId").description("제출 ID"),
								fieldWithPath("attemptNo").description("몇 번째 제출인지(1부터, 영상 교체 포함)"),
								fieldWithPath("result").description("제출 결과. 만든 직후는 PENDING"),
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
	void 승인된_수료의_보상을_받고_만료_시각을_반환한다() throws Exception {
		given(graduationRewardClaimer.claim(42L, TOPIC_ID)).willReturn(TopicGraduationFixture.create(3L, 42L, TOPIC_ID,
				GraduationStatus.APPROVED, REVIEWED_AT, CLAIMED_AT, EXPIRES_AT));

		mockMvc.perform(put("/api/training/topics/{topicId}/graduation/reward", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days").value(3))
				.andExpect(jsonPath("$.claimedAt").value("2026-10-15T09:00:00Z"))
				.andExpect(jsonPath("$.expiresAt").value("2026-10-18T09:00:00Z"))
				.andDo(document("graduation/reward",
						pathParameters(parameterWithName("topicId").description("토픽 ID")),
						responseFields(
								fieldWithPath("days").description("보상 일수"),
								fieldWithPath("claimedAt").description("보상을 받은 시각. 이미 받았으면 처음 받은 시각"),
								fieldWithPath("expiresAt").description("보상으로 지급한 전체 이용권의 만료 시각")
						)
				));
	}

	@Test
	void 승인되지_않은_수료의_보상을_요청하면_409를_반환한다() throws Exception {
		given(graduationRewardClaimer.claim(42L, TOPIC_ID)).willThrow(new GraduationNotApprovedException(TOPIC_ID));

		mockMvc.perform(put("/api/training/topics/{topicId}/graduation/reward", TOPIC_ID)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("GRADUATION_NOT_APPROVED"))
				.andDo(document("graduation/reward-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/graduations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
