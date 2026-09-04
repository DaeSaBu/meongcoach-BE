package com.daesabu.meongcoach.training.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.LessonResult;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 커리큘럼 리스트·세부 조회 API 검증.
 */
@WebMvcTest(TrainingCurriculumController.class)
@AutoConfigureRestDocs
class TrainingCurriculumControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CurriculumFinder curriculumFinder;

	@Test
	void 선택된_토픽과_커리큘럼_목록을_반환한다() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListResult(1L, "앉아", List.of(
				new CurriculumResult(10L, "앉아 1단계", 3, 3, CurriculumStatus.COMPLETED),
				new CurriculumResult(11L, "앉아 2단계", 4, 1, CurriculumStatus.IN_PROGRESS)
		)));

		mockMvc.perform(get("/api/training/curriculums")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
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
	void 인증_주체에서_읽은_사용자로_커리큘럼_조회를_위임한다() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListResult(1L, "앉아", List.of()));

		mockMvc.perform(get("/api/training/curriculums").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(curriculumFinder).should().findCurriculums(42L);
	}

	@Test
	void 커리큘럼이_없는_토픽은_빈_배열과_200을_반환한다() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willReturn(new CurriculumListResult(1L, "앉아", List.of()));

		mockMvc.perform(get("/api/training/curriculums").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1))
				.andExpect(jsonPath("$.curriculums").isArray())
				.andExpect(jsonPath("$.curriculums").isEmpty());
	}

	@Test
	void 등록된_토픽이_없으면_404와_에러_코드를_반환한다() throws Exception {
		given(curriculumFinder.findCurriculums(42L)).willThrow(new TopicNotConfiguredException());

		mockMvc.perform(get("/api/training/curriculums")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_CONFIGURED"))
				.andExpect(jsonPath("$.detail").value("등록된 토픽이 없습니다."))
				.andDo(document("training/curriculum-list-error",
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
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/api/training/curriculums"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 커리큘럼과_레슨_목록을_반환한다() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L)).willReturn(new CurriculumDetailResult(10L, 1L, "앉아 1단계", 1,
				List.of(
						new LessonResult(100L, "손 위의 간식", 1, 5, 3),
						new LessonResult(101L, "간식 없이 앉아", 2, 10, 0)
				)));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
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
	void 인증_주체에서_읽은_사용자로_커리큘럼_세부_조회를_위임한다() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L))
				.willReturn(new CurriculumDetailResult(10L, 1L, "앉아 1단계", 1, List.of()));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(curriculumFinder).should().findCurriculum(42L, 10L);
	}

	@Test
	void 레슨이_없는_커리큘럼은_빈_배열과_200을_반환한다() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 10L))
				.willReturn(new CurriculumDetailResult(10L, 1L, "앉아 1단계", 1, List.of()));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.curriculumId").value(10))
				.andExpect(jsonPath("$.lessons").isArray())
				.andExpect(jsonPath("$.lessons").isEmpty());
	}

	@Test
	void 존재하지_않는_커리큘럼이면_404와_에러_코드를_반환한다() throws Exception {
		given(curriculumFinder.findCurriculum(42L, 999L)).willThrow(new CurriculumNotFoundException(999L));

		mockMvc.perform(get("/api/training/curriculums/{curriculumId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_CURRICULUM_NOT_FOUND"))
				.andDo(document("training/curriculum-detail-error",
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
