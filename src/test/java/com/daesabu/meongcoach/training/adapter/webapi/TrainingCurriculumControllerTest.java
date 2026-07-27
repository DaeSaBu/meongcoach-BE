package com.daesabu.meongcoach.training.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CurriculumDetailView;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListView;
import com.daesabu.meongcoach.training.application.provided.CurriculumView;
import com.daesabu.meongcoach.training.application.provided.LessonView;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 커리큘럼 리스트·세부 조회 API 검증.
 */
@WebMvcTest(TrainingCurriculumController.class)
@AutoConfigureRestDocs
@DisplayName("커리큘럼 조회 API")
class TrainingCurriculumControllerTest {

	private static final String USER_ID_HEADER = "X-User-Id";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CurriculumFinder curriculumFinder;

	@Test
	@DisplayName("선택된 토픽과 커리큘럼 목록을 반환한다")
	void findCurriculumsReturnsTopicWithCurriculums() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListView(1L, "앉아", List.of(
				new CurriculumView(10L, "앉아 1단계", 3, 3, CurriculumStatus.COMPLETED),
				new CurriculumView(11L, "앉아 2단계", 4, 1, CurriculumStatus.IN_PROGRESS)
		)));

		mockMvc.perform(get("/api/training/curriculums").header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1))
				.andExpect(jsonPath("$.topicTitle").value("앉아"))
				.andExpect(jsonPath("$.curriculums[0].curriculumId").value(10))
				.andExpect(jsonPath("$.curriculums[0].curriculumTitle").value("앉아 1단계"))
				.andExpect(jsonPath("$.curriculums[0].totalLessons").value(3))
				.andExpect(jsonPath("$.curriculums[0].completedLessons").value(3))
				.andExpect(jsonPath("$.curriculums[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$.curriculums[1].curriculumId").value(11))
				.andExpect(jsonPath("$.curriculums[1].totalLessons").value(4))
				.andExpect(jsonPath("$.curriculums[1].completedLessons").value(1))
				.andExpect(jsonPath("$.curriculums[1].status").value("IN_PROGRESS"))
				.andDo(document("training/curriculum-list",
						requestHeaders(
								headerWithName(USER_ID_HEADER).description("로그인 사용자 ID")
						),
						responseFields(
								fieldWithPath("topicId").description("커리큘럼 화면에 표시 중인 토픽 ID"),
								fieldWithPath("topicTitle").description("토픽 이름"),
								fieldWithPath("curriculums[]").description("토픽의 커리큘럼 목록. 노출 순서 오름차순"),
								fieldWithPath("curriculums[].curriculumId").description("커리큘럼 ID"),
								fieldWithPath("curriculums[].curriculumTitle").description("커리큘럼 이름"),
								fieldWithPath("curriculums[].totalLessons").description("커리큘럼에 속한 전체 레슨 수"),
								fieldWithPath("curriculums[].completedLessons").description("사용자가 완료한 레슨 수"),
								fieldWithPath("curriculums[].status")
										.description("진행 상태. `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`")
						)
				));
	}

	@Test
	@DisplayName("헤더에서 읽은 사용자로 커리큘럼 조회를 위임한다")
	void findCurriculumsDelegatesWithLoginUser() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListView(1L, "앉아", List.of()));

		mockMvc.perform(get("/api/training/curriculums").header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk());

		then(curriculumFinder).should().findCurriculums(42L);
	}

	@Test
	@DisplayName("커리큘럼이 없는 토픽은 빈 배열과 200을 반환한다")
	void findCurriculumsReturnsEmptyArrayWhenTopicHasNoCurriculum() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListView(1L, "앉아", List.of()));

		mockMvc.perform(get("/api/training/curriculums").header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1))
				.andExpect(jsonPath("$.curriculums").isArray())
				.andExpect(jsonPath("$.curriculums").isEmpty());
	}

	@Test
	@DisplayName("등록된 토픽이 없으면 404와 에러 코드를 반환한다")
	void findCurriculumsReturnsNotFoundWhenNoTopicIsConfigured() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willThrow(new TopicNotConfiguredException());

		mockMvc.perform(get("/api/training/curriculums").header(USER_ID_HEADER, "42"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_CONFIGURED"))
				.andExpect(jsonPath("$.detail").value("등록된 토픽이 없습니다."))
				.andDo(document("training/curriculum-list-error",
						requestHeaders(
								headerWithName(USER_ID_HEADER).description("로그인 사용자 ID")
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
	@DisplayName("X-User-Id 헤더가 없으면 400을 반환한다")
	void findCurriculumsReturnsBadRequestWhenUserIdHeaderIsMissing() throws Exception {
		mockMvc.perform(get("/api/training/curriculums"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	@DisplayName("커리큘럼과 레슨 목록을 반환한다")
	void findCurriculumReturnsCurriculumWithLessons() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L)).willReturn(new CurriculumDetailView(10L, 1L, "앉아 1단계", 1,
				List.of(
						new LessonView(100L, "손 위의 간식", 1, 5, 3),
						new LessonView(101L, "간식 없이 앉아", 2, 10, 0)
				)));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L).header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.curriculumId").value(10))
				.andExpect(jsonPath("$.topicId").value(1))
				.andExpect(jsonPath("$.curriculumTitle").value("앉아 1단계"))
				.andExpect(jsonPath("$.curriculumSortOrder").value(1))
				.andExpect(jsonPath("$.lessons[0].lessonId").value(100))
				.andExpect(jsonPath("$.lessons[0].lessonTitle").value("손 위의 간식"))
				.andExpect(jsonPath("$.lessons[0].lessonSortOrder").value(1))
				.andExpect(jsonPath("$.lessons[0].estimatedMinutes").value(5))
				.andExpect(jsonPath("$.lessons[0].userLessonProgress.completedCount").value(3))
				.andExpect(jsonPath("$.lessons[1].lessonId").value(101))
				.andExpect(jsonPath("$.lessons[1].estimatedMinutes").value(10))
				.andExpect(jsonPath("$.lessons[1].userLessonProgress.completedCount").value(0))
				.andDo(document("training/curriculum-detail",
						requestHeaders(
								headerWithName(USER_ID_HEADER).description("로그인 사용자 ID")
						),
						pathParameters(
								parameterWithName("curriculumId").description("조회할 커리큘럼 ID")
						),
						responseFields(
								fieldWithPath("curriculumId").description("커리큘럼 ID"),
								fieldWithPath("topicId").description("커리큘럼이 속한 토픽 ID"),
								fieldWithPath("curriculumTitle").description("커리큘럼 이름"),
								fieldWithPath("curriculumSortOrder").description("커리큘럼 노출 순서"),
								fieldWithPath("lessons[]").description("커리큘럼의 레슨 목록. 노출 순서 오름차순"),
								fieldWithPath("lessons[].lessonId").description("레슨 ID"),
								fieldWithPath("lessons[].lessonTitle").description("레슨 이름"),
								fieldWithPath("lessons[].lessonSortOrder").description("레슨 노출 순서"),
								fieldWithPath("lessons[].estimatedMinutes").description("예상 소요 시간(분)"),
								fieldWithPath("lessons[].userLessonProgress").description("사용자의 레슨 진행도"),
								fieldWithPath("lessons[].userLessonProgress.completedCount")
										.description("반복 완료 횟수. 기록이 없으면 0")
						)
				));
	}

	@Test
	@DisplayName("헤더에서 읽은 사용자로 커리큘럼 세부 조회를 위임한다")
	void findCurriculumDelegatesWithLoginUser() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L))
				.willReturn(new CurriculumDetailView(10L, 1L, "앉아 1단계", 1, List.of()));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L).header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk());

		then(curriculumFinder).should().findCurriculum(42L, 10L);
	}

	@Test
	@DisplayName("레슨이 없는 커리큘럼은 빈 배열과 200을 반환한다")
	void findCurriculumReturnsEmptyArrayWhenCurriculumHasNoLesson() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L))
				.willReturn(new CurriculumDetailView(10L, 1L, "앉아 1단계", 1, List.of()));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L).header(USER_ID_HEADER, "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.curriculumId").value(10))
				.andExpect(jsonPath("$.lessons").isArray())
				.andExpect(jsonPath("$.lessons").isEmpty());
	}

	@Test
	@DisplayName("존재하지 않는 커리큘럼이면 404와 에러 코드를 반환한다")
	void findCurriculumReturnsNotFoundWhenCurriculumDoesNotExist() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 999L)).willThrow(new CurriculumNotFoundException(999L));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 999L).header(USER_ID_HEADER, "42"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_CURRICULUM_NOT_FOUND"))
				.andDo(document("training/curriculum-detail-error",
						requestHeaders(
								headerWithName(USER_ID_HEADER).description("로그인 사용자 ID")
						),
						pathParameters(
								parameterWithName("curriculumId").description("조회할 커리큘럼 ID")
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
}
