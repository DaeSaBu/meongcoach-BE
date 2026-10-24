package com.daesabu.meongcoach.plan.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.plan.application.provided.CustomPlanCreator;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanLessonResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
	@MockitoBean
	private CurrentPlanFinder currentPlanFinder;

	@Test
	void 현재_플랜과_순서가_있는_커리큘럼_레슨을_반환한다() throws Exception {
		PlanCreateCommand command = new PlanCreateCommand(42L, 3L, "초코의 2주 훈련 플랜", "선택한 훈련을 차근차근 익혀요", 10, 3);
		Plan plan = Plan.create(command, List.of(7L));
		ReflectionTestUtils.setField(plan, "id", 21L);
		ReflectionTestUtils.setField(plan, "createdAt", LocalDateTime.of(2026, 10, 23, 12, 0));
		PlanCurriculumResult curriculum = new PlanCurriculumResult(7L, "현관 매너", "현관에서 차분하게 기다리는 훈련", 2, 1,
				ProgressStatus.IN_PROGRESS, true, List.of(new PlanLessonResult(11L, "앉아서 기다리기", true),
				new PlanLessonResult(12L, "문을 열어도 기다리기", false)));
		given(currentPlanFinder.find(42L)).willReturn(Optional.of(new CurrentPlanResult(plan, List.of(curriculum))));

		mockMvc.perform(get("/api/plans/current").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan.planId").value(21))
				.andExpect(jsonPath("$.plan.completedAt").value(nullValue()))
				.andExpect(jsonPath("$.plan.curriculums[0].locked").value(true))
				.andExpect(jsonPath("$.plan.curriculums[0].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.plan.curriculums[0].lessons[0].completed").value(true))
				.andExpect(jsonPath("$.plan.curriculums[0].lessons[1].lessonId").value(12))
				.andDo(document("plan/current", responseFields(
						fieldWithPath("plan").description("현재 플랜, 활성 플랜 우선, 없으면 가장 최근 완료한 플랜. 둘 다 없으면 null").optional(),
						fieldWithPath("plan.planId").description("플랜 ID"),
						fieldWithPath("plan.dogId").description("플랜 생성 시 선택된 강아지 ID"),
						fieldWithPath("plan.title").description("플랜 제목"),
						fieldWithPath("plan.goal").description("플랜 목표"),
						fieldWithPath("plan.dailyMinutes").description("하루 훈련 시간, 20은 20분 이상"),
						fieldWithPath("plan.daysPerWeek").description("주당 훈련 일수"),
						fieldWithPath("plan.status").description("ACTIVE 또는 COMPLETED"),
						fieldWithPath("plan.createdAt").description("생성 시각"),
						fieldWithPath("plan.completedAt").type(JsonFieldType.STRING).optional().description("완료 시각, 미완료 시 null"),
						fieldWithPath("plan.curriculums").description("플랜에 저장한 순서의 커리큘럼 목록, 배열 순번이 카드 번호"),
						fieldWithPath("plan.curriculums[].curriculumId").description("커리큘럼 ID"),
						fieldWithPath("plan.curriculums[].title").description("커리큘럼 제목"),
						fieldWithPath("plan.curriculums[].description").description("커리큘럼 설명"),
						fieldWithPath("plan.curriculums[].totalLessons").description("전체 레슨 수"),
						fieldWithPath("plan.curriculums[].completedLessons").description("사용자가 완료한 레슨 수"),
						fieldWithPath("plan.curriculums[].status").description("NOT_STARTED, IN_PROGRESS, COMPLETED"),
						fieldWithPath("plan.curriculums[].locked").description("이용권이 없어 잠금 표시가 필요한지 여부, 조회 자체는 허용"),
						fieldWithPath("plan.curriculums[].lessons").description("교육 순서의 레슨 목록"),
						fieldWithPath("plan.curriculums[].lessons[].lessonId").description("기존 레슨 상세 조회에 사용하는 ID"),
						fieldWithPath("plan.curriculums[].lessons[].title").description("레슨 제목"),
						fieldWithPath("plan.curriculums[].lessons[].completed").description("사용자의 레슨 완료 여부"))));
	}

	@Test
	void 현재_플랜이_없으면_200과_null을_반환한다() throws Exception {
		given(currentPlanFinder.find(42L)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/plans/current").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan").value(nullValue()))
				.andDo(document("plan/current-empty", responseFields(
						fieldWithPath("plan").type(JsonFieldType.OBJECT).optional().description("활성 및 완료한 플랜이 없으면 null"))));
	}

	@Test
	void 현재_플랜_조회에_인증이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/plans/current"))
				.andExpect(status().isUnauthorized())
				.andDo(document("plan/current-error", responseFields(ERROR_FIELDS)));

		verifyNoInteractions(currentPlanFinder);
	}


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
