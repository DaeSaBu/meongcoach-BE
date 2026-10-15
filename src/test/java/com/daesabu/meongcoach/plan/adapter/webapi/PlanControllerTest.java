package com.daesabu.meongcoach.plan.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.daesabu.meongcoach.plan.application.provided.PlanCompleter;
import com.daesabu.meongcoach.plan.application.provided.PlanCreator;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult.ReflectedTopic;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Item;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Progress;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Today;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Week;
import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanController.class)
@AutoConfigureRestDocs
class PlanControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	private static final String CREATE_BODY = """
			{
			  "dogId": 7,
			  "mode": "AI",
			  "topicIds": null,
			  "expectation": "산책할 때 줄을 당기지 않았으면 좋겠어요",
			  "dailyMinutes": 10,
			  "daysPerWeek": 3,
			  "replace": false
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlanCreator planCreator;

	@MockitoBean
	private PlanFinder planFinder;

	@MockitoBean
	private PlanCompleter planCompleter;

	@Test
	void 플랜을_만들면_201과_플랜_상세를_반환한다() throws Exception {
		given(planCreator.create(any(Long.class), any(PlanCreateRequest.class))).willReturn(detail(null));

		mockMvc.perform(post("/api/plans")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CREATE_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planId").value(100))
				.andExpect(jsonPath("$.dogName").value("초코"))
				.andExpect(jsonPath("$.mode").value("AI"))
				.andExpect(jsonPath("$.generator").value("FALLBACK"))
				.andExpect(jsonPath("$.adjustedReason").doesNotExist())
				.andExpect(jsonPath("$.progress.totalItems").value(2))
				.andExpect(jsonPath("$.today.topicId").value(11))
				.andExpect(jsonPath("$.weeks[0].items[0].curriculumTitle").value("앉아 기초"))
				.andDo(document("plan/create",
						requestFields(
								fieldWithPath("dogId").description("필수 입력. 플랜을 만들 강아지 ID"),
								fieldWithPath("mode").description("필수 입력. 생성 방식(AI, MANUAL)"),
								fieldWithPath("topicIds").optional().type(JsonFieldType.ARRAY)
										.description("MANUAL이면 필수. 직접 고른 토픽 ID 1~3개. AI이면 생략하거나 null"),
								fieldWithPath("expectation").optional().type(JsonFieldType.STRING)
										.description("AI이면 필수. 교육 기대 사항(5~200자). 강아지 프로필의 기대 사항에도 반영. MANUAL이면 생략하거나 null"),
								fieldWithPath("dailyMinutes").description("필수 입력. 하루 교육 시간(분, 1~60)"),
								fieldWithPath("daysPerWeek").description("필수 입력. 주당 교육 일수(1~7)"),
								fieldWithPath("replace").description("진행 중인 플랜을 포기하고 새로 만들지 여부. false인데 진행 중인 플랜이 있으면 409")
						),
						responseFields(detailFields("", true))
				));

		ArgumentCaptor<PlanCreateRequest> captor = ArgumentCaptor.forClass(PlanCreateRequest.class);
		then(planCreator).should().create(eq(42L), captor.capture());
		assertThat(captor.getValue().dogId()).isEqualTo(7L);
		assertThat(captor.getValue().mode()).isEqualTo("AI");
		assertThat(captor.getValue().replace()).isFalse();
	}

	@Test
	void 진행_중인_플랜이_있으면_생성은_409와_에러_코드를_반환한다() throws Exception {
		given(planCreator.create(any(Long.class), any(PlanCreateRequest.class)))
				.willThrow(new PlanAlreadyActiveException());

		mockMvc.perform(post("/api/plans")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CREATE_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("PLAN_ALREADY_ACTIVE"))
				.andDo(document("plan/create-error", errorFields()));
	}

	@Test
	void 하루_분량이_범위를_벗어나면_생성은_400을_반환하고_위임하지_않는다() throws Exception {
		mockMvc.perform(post("/api/plans")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(CREATE_BODY.replace("\"dailyMinutes\": 10", "\"dailyMinutes\": 0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("dailyMinutes"));

		then(planCreator).shouldHaveNoInteractions();
	}

	@Test
	void 인증_정보가_없으면_생성은_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/plans")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CREATE_BODY))
				.andExpect(status().isUnauthorized());

		then(planCreator).shouldHaveNoInteractions();
	}

	@Test
	void 진행_중인_플랜이_있으면_현재_플랜_조회는_상세를_반환한다() throws Exception {
		given(planFinder.findCurrent(42L)).willReturn(new CurrentPlanResult(true, detail(PlanAdjustedReason.REVIEW)));

		mockMvc.perform(get("/api/plans/current")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasCompletedPlan").value(true))
				.andExpect(jsonPath("$.plan.planId").value(100))
				.andExpect(jsonPath("$.plan.adjustedReason").value("REVIEW"))
				.andExpect(jsonPath("$.plan.weeks[1].status").value("NOT_STARTED"))
				.andDo(document("plan/current",
						responseFields(currentFields(detailFields("plan.", false)))
				));
	}

	@Test
	void 진행_중인_플랜이_없으면_현재_플랜_조회는_plan이_null이다() throws Exception {
		given(planFinder.findCurrent(42L)).willReturn(new CurrentPlanResult(false, null));

		mockMvc.perform(get("/api/plans/current")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasCompletedPlan").value(false))
				.andExpect(jsonPath("$.plan").value((Object) null))
				.andDo(document("plan/current-empty",
						responseFields(
								fieldWithPath("hasCompletedPlan").description("마무리한 플랜이 한 번이라도 있는지 여부"),
								fieldWithPath("plan").optional().type(JsonFieldType.OBJECT)
										.description("진행 중인 플랜 상세. 없으면 null")
						)
				));
	}

	@Test
	void 플랜을_마무리하면_201과_결과를_반환한다() throws Exception {
		given(planCompleter.complete(any(Long.class), any(Long.class), any(PlanCompleteRequest.class)))
				.willReturn(new PlanCompletionResult(100L, "초코의 2주 앉아 플랜", PlanOutcome.ACHIEVED, 2, 2, 4, 6,
						List.of(new ReflectedTopic(11L, "앉아"))));

		mockMvc.perform(post("/api/plans/{planId}/completion", 100L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "outcome": "ACHIEVED" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planId").value(100))
				.andExpect(jsonPath("$.outcome").value("ACHIEVED"))
				.andExpect(jsonPath("$.daysTaken").value(6))
				.andExpect(jsonPath("$.reflectedTopics[0].topicTitle").value("앉아"))
				.andDo(document("plan/complete",
						pathParameters(parameterWithName("planId").description("마무리할 플랜 ID")),
						requestFields(
								fieldWithPath("outcome").description("필수 입력. 결과(ACHIEVED 달성, PARTIAL 일부 달성, NOT_YET 아직)")
						),
						responseFields(
								fieldWithPath("planId").description("마무리한 플랜 ID"),
								fieldWithPath("title").description("플랜 제목"),
								fieldWithPath("outcome").description("마무리 결과"),
								fieldWithPath("completedCurriculums").description("완료한 커리큘럼 수"),
								fieldWithPath("totalCurriculums").description("플랜의 전체 커리큘럼 수"),
								fieldWithPath("totalLessons").description("플랜의 전체 레슨 수"),
								fieldWithPath("daysTaken").description("시작일부터 오늘까지 걸린 일수(시작일 포함)"),
								fieldWithPath("reflectedTopics[]")
										.description("배운 토픽에 반영한 토픽 목록. 아직(NOT_YET)이면 빈 배열"),
								fieldWithPath("reflectedTopics[].topicId").description("반영한 토픽 ID"),
								fieldWithPath("reflectedTopics[].topicTitle").description("반영한 토픽 이름")
						)
				));

		then(planCompleter).should().complete(eq(42L),
				eq(100L), any(PlanCompleteRequest.class));
	}

	@Test
	void 플랜이_없으면_마무리는_404와_에러_코드를_반환한다() throws Exception {
		given(planCompleter.complete(any(Long.class), any(Long.class), any(PlanCompleteRequest.class)))
				.willThrow(new PlanNotFoundException());

		mockMvc.perform(post("/api/plans/{planId}/completion", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "outcome": "ACHIEVED" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"))
				.andDo(document("plan/complete-error", errorFields()));
	}

	@Test
	void 결과가_비어_있으면_마무리는_400을_반환하고_위임하지_않는다() throws Exception {
		mockMvc.perform(post("/api/plans/{planId}/completion", 100L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "outcome": " " }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));

		then(planCompleter).shouldHaveNoInteractions();
	}

	private Snippet errorFields() {
		return responseFields(
				fieldWithPath("title").description("HTTP 상태 이름"),
				fieldWithPath("status").description("HTTP 상태 코드"),
				fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
				fieldWithPath("instance").description("에러가 발생한 요청 경로"),
				fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
				fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
		);
	}

	private List<FieldDescriptor> currentFields(List<FieldDescriptor> planFields) {
		List<FieldDescriptor> fields = new ArrayList<>();
		fields.add(fieldWithPath("hasCompletedPlan").description("마무리한 플랜이 한 번이라도 있는지 여부"));
		fields.add(fieldWithPath("plan").optional().type(JsonFieldType.OBJECT)
				.description("진행 중인 플랜 상세. 없으면 null"));
		fields.addAll(planFields);
		return fields;
	}

	private List<FieldDescriptor> detailFields(String prefix, boolean adjustedReasonNull) {
		List<FieldDescriptor> fields = new ArrayList<>();
		fields.add(fieldWithPath(prefix + "planId").description("플랜 ID"));
		fields.add(fieldWithPath(prefix + "dogId").description("강아지 ID"));
		fields.add(fieldWithPath(prefix + "dogName").description("강아지 이름"));
		fields.add(fieldWithPath(prefix + "mode").description("생성 방식(AI, MANUAL)"));
		fields.add(fieldWithPath(prefix + "title").description("플랜 제목"));
		fields.add(fieldWithPath(prefix + "goal").description("플랜 목표"));
		fields.add(fieldWithPath(prefix + "reason").description("이렇게 짠 이유"));
		fields.add(fieldWithPath(prefix + "generator").description("실제로 플랜을 만든 주체(AI, FALLBACK, MANUAL)"));
		fields.add(fieldWithPath(prefix + "adjustedReason").optional().type(JsonFieldType.STRING)
				.description("기간·구성을 조정한 사유(DURATION_SHORTENED, REVIEW). 조정하지 않았으면 null"));
		fields.add(fieldWithPath(prefix + "durationWeeks").description("플랜 기간(주)"));
		fields.add(fieldWithPath(prefix + "dailyMinutes").description("하루 교육 시간(분)"));
		fields.add(fieldWithPath(prefix + "daysPerWeek").description("주당 교육 일수"));
		fields.add(fieldWithPath(prefix + "startedOn").description("시작일(yyyy-MM-dd)"));
		fields.add(fieldWithPath(prefix + "targetEndOn").description("목표 종료일(yyyy-MM-dd)"));
		fields.add(fieldWithPath(prefix + "daysLeft").description("목표 종료일까지 남은 일수. 지났으면 음수"));
		fields.add(fieldWithPath(prefix + "currentWeekNo").description("실제 진행 기준 현재 주차"));
		fields.add(fieldWithPath(prefix + "expectedWeekNo").description("시작일 기준으로 지금쯤이어야 하는 주차"));
		fields.add(fieldWithPath(prefix + "progress").description("커리큘럼 진행 집계"));
		fields.add(fieldWithPath(prefix + "progress.completedItems").description("완료한 커리큘럼 수"));
		fields.add(fieldWithPath(prefix + "progress.totalItems").description("플랜의 전체 커리큘럼 수"));
		fields.add(fieldWithPath(prefix + "today").optional().type(JsonFieldType.OBJECT)
				.description("오늘 이어서 할 레슨. 모두 완료했으면 null"));
		fields.add(fieldWithPath(prefix + "today.curriculumId").description("커리큘럼 ID"));
		fields.add(fieldWithPath(prefix + "today.curriculumTitle").description("커리큘럼 이름"));
		fields.add(fieldWithPath(prefix + "today.topicId").description("토픽 ID"));
		fields.add(fieldWithPath(prefix + "today.topicTitle").description("토픽 이름"));
		fields.add(fieldWithPath(prefix + "today.lessonId").description("레슨 ID"));
		fields.add(fieldWithPath(prefix + "today.lessonTitle").description("레슨 이름"));
		fields.add(fieldWithPath(prefix + "today.estimatedMinutes").optional().type(JsonFieldType.NUMBER)
				.description("레슨 예상 소요 시간(분)"));
		fields.add(fieldWithPath(prefix + "weeks[]").description("주차별 구성"));
		fields.add(fieldWithPath(prefix + "weeks[].weekNo").description("주차(1부터)"));
		fields.add(fieldWithPath(prefix + "weeks[].theme").description("주차 테마"));
		fields.add(fieldWithPath(prefix + "weeks[].milestone").description("주차 마일스톤"));
		fields.add(fieldWithPath(prefix + "weeks[].status")
				.description("주차 진행 상태(NOT_STARTED, IN_PROGRESS, COMPLETED)"));
		fields.add(fieldWithPath(prefix + "weeks[].items[]").description("주차에 담긴 커리큘럼 목록"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].curriculumId").description("커리큘럼 ID"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].curriculumTitle").description("커리큘럼 이름"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].topicId").description("토픽 ID"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].topicTitle").description("토픽 이름"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].totalLessons").description("커리큘럼의 전체 레슨 수"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].completedLessons").description("완료한 레슨 수"));
		fields.add(fieldWithPath(prefix + "weeks[].items[].status")
				.description("커리큘럼 진행 상태(NOT_STARTED, IN_PROGRESS, COMPLETED)"));
		return fields;
	}

	private PlanDetailResult detail(PlanAdjustedReason adjustedReason) {
		Item first = new Item(21L, "앉아 기초", 11L, "앉아", 2, 1, CurriculumStatus.IN_PROGRESS);
		Item second = new Item(22L, "앉아 심화", 11L, "앉아", 2, 0, CurriculumStatus.NOT_STARTED);
		List<Week> weeks = List.of(
				new Week(1, "앉아", "앉아 기초까지 해내기", CurriculumStatus.IN_PROGRESS, List.of(first)),
				new Week(2, "앉아", "앉아 심화까지 해내기", CurriculumStatus.NOT_STARTED, List.of(second)));
		Today today = new Today(21L, "앉아 기초", 11L, "앉아", 211L, "앉아 기초 레슨2", 5);
		return new PlanDetailResult(100L, 7L, "초코", PlanMode.AI, "초코의 2주 앉아 플랜", "2주 동안 앉아를 차근차근 익혀요",
				"이렇게 짰어요", PlanGeneratorType.FALLBACK, adjustedReason, 2, 10, 3, LocalDate.of(2026, 10, 15),
				LocalDate.of(2026, 10, 28), 13, 1, 1, new Progress(0, 2), today, weeks);
	}
}
