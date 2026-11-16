package com.daesabu.meongcoach.training.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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

import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.training.application.provided.CurriculumProgressRegister;
import com.daesabu.meongcoach.training.application.provided.TrainingCalendarFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumProgressResultRequest;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarDayResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarLessonResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarTopicResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressAlreadySucceededException;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.InvalidCurriculumProgressResultException;
import com.daesabu.meongcoach.training.domain.exception.InvalidTrainingCalendarPeriodException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainingCalendarController.class)
@AutoConfigureRestDocs
class TrainingCalendarControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 30);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TrainingCalendarFinder trainingCalendarFinder;

	@MockitoBean
	private CurriculumProgressRegister curriculumProgressRegister;

	@Test
	void 훈련_달력을_조회하면_날짜별_훈련_기록을_반환한다() throws Exception {
		Topic topic = topic(1301L, "산책", "footprints");
		Curriculum curriculum = curriculum(topic, 130103L, "집안 산책하기");
		Lesson planLesson = lesson(curriculum, 13010302L, "보호자 따라오게 하기");
		Lesson libraryLesson = lesson(curriculum, 13010301L, "강아지 따라다녀주기");
		TrainingCalendarDayResult day = new TrainingCalendarDayResult(LocalDate.of(2026, 11, 16),
				List.of(topicResult(topic, curriculum, CurriculumProgressStatus.SUCCEEDED, planLesson, 2)),
				List.of(topicResult(topic, curriculum, CurriculumProgressStatus.SUCCEEDED, libraryLesson, 1)));
		TrainingCalendarResult result = new TrainingCalendarResult(LocalDate.of(2026, 10, 17), List.of(day));
		given(trainingCalendarFinder.find(42L, 3L, FROM, TO)).willReturn(result);

		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstTrainedDate").value("2026-10-17"))
				.andExpect(jsonPath("$.days[0].date").value("2026-11-16"))
				.andExpect(jsonPath("$.days[0].plan[0].topicId").value(1301))
				.andExpect(jsonPath("$.days[0].plan[0].topicTitle").value("산책"))
				.andExpect(jsonPath("$.days[0].plan[0].topicIconUrl").value("footprints"))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].curriculumId").value(130103))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].curriculumTitle").value("집안 산책하기"))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].status").value("SUCCEEDED"))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].lessons[0].lessonId").value(13010302))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].lessons[0].lessonTitle")
						.value("보호자 따라오게 하기"))
				.andExpect(jsonPath("$.days[0].plan[0].curriculums[0].lessons[0].completedCount").value(2))
				.andExpect(jsonPath("$.days[0].library[0].curriculums[0].lessons[0].lessonId").value(13010301))
				.andDo(document("training/calendar",
						queryParameters(
								parameterWithName("dogId").description("달력을 조회할 강아지 ID"),
								parameterWithName("from").description("조회 시작일(yyyy-MM-dd)"),
								parameterWithName("to").description("조회 종료일(yyyy-MM-dd). 시작일부터 최대 62일")
						),
						responseFields(calendarFields())
				));
	}

	@Test
	void 훈련한_적이_없으면_처음_훈련한_날은_null이고_날짜_목록은_빈_배열이다() throws Exception {
		given(trainingCalendarFinder.find(42L, 3L, FROM, TO)).willReturn(new TrainingCalendarResult(null, List.of()));

		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstTrainedDate").value(nullValue()))
				.andExpect(jsonPath("$.days", hasSize(0)));
	}

	@Test
	void 그날_한쪽_출처의_기록이_없으면_빈_배열로_반환한다() throws Exception {
		Topic topic = topic(1301L, "산책", "footprints");
		Curriculum curriculum = curriculum(topic, 130103L, "집안 산책하기");
		Lesson lesson = lesson(curriculum, 13010302L, "보호자 따라오게 하기");
		TrainingCalendarDayResult day = new TrainingCalendarDayResult(LocalDate.of(2026, 11, 16),
				List.of(topicResult(topic, curriculum, CurriculumProgressStatus.PENDING, lesson, 1)), List.of());
		given(trainingCalendarFinder.find(42L, 3L, FROM, TO))
				.willReturn(new TrainingCalendarResult(LocalDate.of(2026, 11, 16), List.of(day)));

		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days[0].plan", hasSize(1)))
				.andExpect(jsonPath("$.days[0].library", hasSize(0)));
	}

	@Test
	void 조회_기간이_올바르지_않으면_400과_에러_코드를_반환한다() throws Exception {
		given(trainingCalendarFinder.find(42L, 3L, TO, FROM)).willThrow(new InvalidTrainingCalendarPeriodException());

		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026-11-30")
						.queryParam("to", "2026-11-01")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("TRAINING_INVALID_CALENDAR_PERIOD"))
				.andDo(document("training/calendar-error",
						queryParameters(
								parameterWithName("dogId").description("달력을 조회할 강아지 ID"),
								parameterWithName("from").description("조회 시작일(yyyy-MM-dd)"),
								parameterWithName("to").description("조회 종료일(yyyy-MM-dd). 시작일부터 최대 62일")
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
	void 본인_강아지가_아니면_404와_에러_코드를_반환한다() throws Exception {
		given(trainingCalendarFinder.find(42L, 999L, FROM, TO)).willThrow(new DogNotFoundException(999L));

		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "999")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"));
	}

	@Test
	void dogId가_없으면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/calendar")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 날짜_형식이_올바르지_않으면_400을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026/11/01")
						.queryParam("to", "2026-11-30")
						.principal(CURRENT_USER))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 달력_조회_시_인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/calendar")
						.queryParam("dogId", "3")
						.queryParam("from", "2026-11-01")
						.queryParam("to", "2026-11-30"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 훈련_결과를_기록하면_204를_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "SUCCEEDED"}
								"""))
				.andExpect(status().isNoContent())
				.andDo(document("training/calendar-result",
						pathParameters(
								parameterWithName("date").description("훈련한 날(yyyy-MM-dd)"),
								parameterWithName("curriculumId").description("그날 훈련한 커리큘럼 ID")
						),
						queryParameters(
								parameterWithName("dogId").description("훈련한 강아지 ID")
						),
						requestFields(
								fieldWithPath("result").description("훈련 결과(SUCCEEDED, FAILED)")
						)
				));

		then(curriculumProgressRegister).should().recordResult(42L, 3L, 130103L, LocalDate.of(2026, 11, 16),
				new CurriculumProgressResultRequest("SUCCEEDED"));
	}

	@Test
	void 성공한_날을_실패로_바꾸려_하면_409와_에러_코드를_반환한다() throws Exception {
		willThrow(new CurriculumProgressAlreadySucceededException()).given(curriculumProgressRegister)
				.recordResult(42L, 3L, 130103L, LocalDate.of(2026, 11, 16),
						new CurriculumProgressResultRequest("FAILED"));

		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "FAILED"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("TRAINING_CURRICULUM_PROGRESS_ALREADY_SUCCEEDED"))
				.andDo(document("training/calendar-result-error",
						pathParameters(
								parameterWithName("date").description("훈련한 날(yyyy-MM-dd)"),
								parameterWithName("curriculumId").description("그날 훈련한 커리큘럼 ID")
						),
						queryParameters(
								parameterWithName("dogId").description("훈련한 강아지 ID")
						),
						requestFields(
								fieldWithPath("result").description("훈련 결과(SUCCEEDED, FAILED)")
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
	void 훈련_결과가_SUCCEEDED나_FAILED가_아니면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new InvalidCurriculumProgressResultException()).given(curriculumProgressRegister)
				.recordResult(42L, 3L, 130103L, LocalDate.of(2026, 11, 16),
						new CurriculumProgressResultRequest("PENDING"));

		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "PENDING"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TRAINING_INVALID_CURRICULUM_PROGRESS_RESULT"));
	}

	@Test
	void 그날_훈련한_기록이_없으면_404와_에러_코드를_반환한다() throws Exception {
		willThrow(new CurriculumProgressNotFoundException()).given(curriculumProgressRegister)
				.recordResult(42L, 3L, 130103L, LocalDate.of(2026, 11, 16),
						new CurriculumProgressResultRequest("SUCCEEDED"));

		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "SUCCEEDED"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_CURRICULUM_PROGRESS_NOT_FOUND"));
	}

	@Test
	void 훈련_결과가_비어_있으면_400을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": ""}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 훈련_결과_기록에_dogId가_없으면_400을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "SUCCEEDED"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 훈련_결과_기록_시_인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/calendar/{date}/curriculums/{curriculumId}/result", "2026-11-16", 130103L)
						.queryParam("dogId", "3")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"result": "SUCCEEDED"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private List<FieldDescriptor> calendarFields() {
		List<FieldDescriptor> fields = new ArrayList<>();
		fields.add(fieldWithPath("firstTrainedDate").type(JsonFieldType.STRING).optional()
				.description("이 강아지가 처음 훈련한 날(yyyy-MM-dd). 조회 기간과 무관하며 훈련한 적이 없으면 null"));
		fields.add(fieldWithPath("days").description("훈련한 날만 담은 목록. 날짜 오름차순"));
		fields.add(fieldWithPath("days[].date").description("훈련한 날(yyyy-MM-dd, Asia/Seoul 기준)"));
		fields.add(fieldWithPath("days[].plan").description("그날 플랜으로 훈련한 토픽 목록. 없으면 빈 배열"));
		fields.addAll(topicFields("days[].plan[]"));
		fields.add(fieldWithPath("days[].library").description("그날 라이브러리에서 훈련한 토픽 목록. 없으면 빈 배열"));
		fields.addAll(topicFields("days[].library[]"));
		return fields;
	}

	private List<FieldDescriptor> topicFields(String prefix) {
		return List.of(
				fieldWithPath(prefix + ".topicId").description("토픽 ID"),
				fieldWithPath(prefix + ".topicTitle").description("토픽 제목"),
				fieldWithPath(prefix + ".topicIconUrl").description("토픽 아이콘"),
				fieldWithPath(prefix + ".curriculums").description("그날 훈련한 커리큘럼 목록. 교육 순서"),
				fieldWithPath(prefix + ".curriculums[].curriculumId").description("커리큘럼 ID"),
				fieldWithPath(prefix + ".curriculums[].curriculumTitle").description("커리큘럼 제목"),
				fieldWithPath(prefix + ".curriculums[].status")
						.description("그날의 훈련 결과(PENDING, SUCCEEDED, FAILED). 같은 날 같은 커리큘럼은 plan과 library에서 같은 값"),
				fieldWithPath(prefix + ".curriculums[].lessons").description("그날 훈련한 레슨 목록. 교육 순서"),
				fieldWithPath(prefix + ".curriculums[].lessons[].lessonId").description("레슨 ID"),
				fieldWithPath(prefix + ".curriculums[].lessons[].lessonTitle").description("레슨 제목"),
				fieldWithPath(prefix + ".curriculums[].lessons[].completedCount")
						.description("그날까지 이 레슨을 훈련한 날 수. 출처와 무관하게 하루에 한 번만 센다")
		);
	}

	private TrainingCalendarTopicResult topicResult(Topic topic, Curriculum curriculum,
			CurriculumProgressStatus status, Lesson lesson, int trainedDays) {
		TrainingCalendarLessonResult lessonResult = new TrainingCalendarLessonResult(lesson, trainedDays);
		TrainingCalendarCurriculumResult curriculumResult = new TrainingCalendarCurriculumResult(curriculum, status,
				List.of(lessonResult));
		return new TrainingCalendarTopicResult(topic, List.of(curriculumResult));
	}

	private Topic topic(Long id, String title, String iconUrl) {
		Topic topic = TopicFixture.create(null, title, 1, null, null, iconUrl);
		ReflectionTestUtils.setField(topic, "id", id);
		return topic;
	}

	private Curriculum curriculum(Topic topic, Long id, String title) {
		Curriculum curriculum = CurriculumFixture.create(topic, title, 1, null, null);
		ReflectionTestUtils.setField(curriculum, "id", id);
		return curriculum;
	}

	private Lesson lesson(Curriculum curriculum, Long id, String title) {
		Lesson lesson = LessonFixture.create(curriculum, title, 1, 5);
		ReflectionTestUtils.setField(lesson, "id", id);
		return lesson;
	}
}
