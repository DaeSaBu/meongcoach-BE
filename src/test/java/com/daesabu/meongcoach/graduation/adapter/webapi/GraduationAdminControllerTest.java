package com.daesabu.meongcoach.graduation.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.TopicGraduationFixture;
import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import com.daesabu.meongcoach.shared.security.AdminKeyInterceptor;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraduationAdminController.class)
@AutoConfigureRestDocs
class GraduationAdminControllerTest {

	private static final String ADMIN_KEY = "test-admin-key";
	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-14T05:03:00Z");
	private static final Instant REVIEWED_AT = Instant.parse("2026-10-15T01:00:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T01:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/550e8400.mp4";

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
	private GraduationReviewer graduationReviewer;

	@Test
	void 수료를_승인하고_지급한_이용권_만료_시각을_반환한다() throws Exception {
		given(graduationReviewer.review(31L, new GraduationReviewRequest("APPROVED", "끝까지 함께 시도했어요!")))
				.willReturn(TopicGraduationFixture.create(31L, 42L, 7L, GraduationStatus.APPROVED, 2, VIDEO_KEY,
						SUBMITTED_AT, REVIEWED_AT, "끝까지 함께 시도했어요!", EXPIRES_AT));

		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"APPROVED\", \"note\": \"끝까지 함께 시도했어요!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduationId").value(31))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.reviewedAt").value("2026-10-15T01:00:00Z"))
				.andExpect(jsonPath("$.reviewNote").value("끝까지 함께 시도했어요!"))
				.andExpect(jsonPath("$.rewardExpiresAt").value("2026-10-18T01:00:00Z"))
				.andDo(document("graduation/admin-review",
						requestHeaders(headerWithName(AdminKeyInterceptor.HEADER).description("운영자 키(ADMIN_API_KEY)")),
						pathParameters(parameterWithName("graduationId").description("판정할 토픽 수료 ID. Slack 알림의 graduationId")),
						requestFields(
								fieldWithPath("result").description("필수 입력. APPROVED(승인) 또는 REVISION_REQUESTED(보완 요청)"),
								fieldWithPath("note").optional().description(
										"운영자 메모(최대 500자). 앱에 그대로 보인다. 보완 요청에는 필수, 승인에는 선택")
						),
						responseFields(
								fieldWithPath("graduationId").description("토픽 수료 ID"),
								fieldWithPath("status").description("판정 뒤 수료 상태. APPROVED 또는 REVISION_REQUESTED"),
								fieldWithPath("reviewedAt").description("판정 시각(ISO-8601, UTC)"),
								fieldWithPath("reviewNote").optional().description("저장한 운영자 메모. 없으면 null"),
								fieldWithPath("rewardExpiresAt").optional().description(
										"승인 때 지급한 전체 이용권 3일의 만료 시각. 보완 요청이면 null")
						)
				));
	}

	@Test
	void 보완_요청에_메모가_없으면_400을_반환한다() throws Exception {
		given(graduationReviewer.review(31L, new GraduationReviewRequest("REVISION_REQUESTED", null)))
				.willThrow(new ReviewNoteRequiredException());

		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"REVISION_REQUESTED\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("GRADUATION_REVIEW_NOTE_REQUIRED"))
				.andDo(document("graduation/admin-review-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 이용권_지급에_실패하면_502를_반환한다() throws Exception {
		given(graduationReviewer.review(31L, new GraduationReviewRequest("APPROVED", null)))
				.willThrow(new EntitlementProviderUnavailableException());

		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"APPROVED\"}"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("ENTITLEMENT_PROVIDER_UNAVAILABLE"));
	}

	@Test
	void 메모가_500자를_넘으면_검증에_실패한다() throws Exception {
		String longNote = "가".repeat(501);

		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, ADMIN_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"REVISION_REQUESTED\", \"note\": \"" + longNote + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 운영자_키가_없으면_401을_반환하고_판정하지_않는다() throws Exception {
		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"APPROVED\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(graduationReviewer).should(never()).review(ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	@Test
	void 운영자_키가_다르면_401을_반환한다() throws Exception {
		mockMvc.perform(put("/api/admin/graduations/{graduationId}/review", 31L)
						.header(AdminKeyInterceptor.HEADER, "wrong-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\": \"APPROVED\"}"))
				.andExpect(status().isUnauthorized());
	}
}
