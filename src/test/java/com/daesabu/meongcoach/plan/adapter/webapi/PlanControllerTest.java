package com.daesabu.meongcoach.plan.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.plan.application.provided.CustomPlanCreator;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanController.class)
@AutoConfigureRestDocs
class PlanControllerTest {

	private static final Principal CURRENT_USER = () -> "42";
	private static final CustomPlanCreateRequest REQUEST = new CustomPlanCreateRequest(List.of(7L, 8L), 10, 3);
	private static final String JSON = "{\"topicIds\": [7, 8], \"dailyMinutes\": 10, \"daysPerWeek\": 3}";
	private static final FieldDescriptor[] ERROR_FIELDS = {
			fieldWithPath("title").description("HTTP 상태 이름"),
			fieldWithPath("status").description("HTTP 상태 코드"),
			fieldWithPath("detail").description("에러 설명"),
			fieldWithPath("instance").description("요청 경로"),
			fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
			fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
	};

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private CustomPlanCreator customPlanCreator;

	@Test
	void 로그인한_사용자의_커스텀_플랜을_생성하고_ID를_반환한다() throws Exception {
		Plan plan = mock(Plan.class);
		given(plan.getId()).willReturn(21L);
		given(customPlanCreator.create(42L, REQUEST)).willReturn(plan);

		mockMvc.perform(post("/api/plans/custom").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planId").value(21))
				.andDo(document("plan/custom-create",
						requestFields(
								fieldWithPath("topicIds").description("선택한 토픽 ID 목록, 1~3개. 미완료 커리큘럼은 교육 순서로 포함"),
								fieldWithPath("dailyMinutes").description("하루 훈련 시간, 5, 10, 20 중 하나. 20은 20분 이상"),
								fieldWithPath("daysPerWeek").description("주당 훈련 일수, 3, 5, 7 중 하나")),
						responseFields(fieldWithPath("planId").description("생성된 플랜 ID"))));
	}

	@Test
	void 미완료_커리큘럼이_없으면_400을_반환한다() throws Exception {
		given(customPlanCreator.create(42L, REQUEST)).willThrow(new EmptyPlanCurriculumsException());

		mockMvc.perform(post("/api/plans/custom").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PLAN_EMPTY_CURRICULUMS"))
				.andDo(document("plan/custom-create-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 저장이_충돌하면_409를_반환한다() throws Exception {
		given(customPlanCreator.create(42L, REQUEST)).willThrow(
				new PlanCreationConflictException(new DataIntegrityViolationException("충돌")));

		mockMvc.perform(post("/api/plans/custom").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLAN_CREATION_CONFLICT"))
				.andDo(document("plan/custom-create-conflict-error", responseFields(ERROR_FIELDS)));
	}

	@Test
	void 필수_입력이_없으면_생성_서비스를_호출하지_않는다() throws Exception {
		mockMvc.perform(post("/api/plans/custom").principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(customPlanCreator);
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/plans/custom").contentType(MediaType.APPLICATION_JSON).content(JSON))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(customPlanCreator);
	}
}
