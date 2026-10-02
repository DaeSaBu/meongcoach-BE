package com.daesabu.meongcoach.training.adapter.webapi;

import static com.daesabu.meongcoach.training.domain.MediaType.IMAGE;
import static com.daesabu.meongcoach.training.domain.MediaType.VIDEO;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CardMediaResult;
import com.daesabu.meongcoach.training.application.provided.CardResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.LessonResult;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainingController.class)
@AutoConfigureRestDocs
class TrainingControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TrainingCategoryFinder trainingCategoryFinder;

	@MockitoBean
	private TopicSelector topicSelector;

	@MockitoBean
	private CurriculumFinder curriculumFinder;

	@MockitoBean
	private LessonFinder lessonFinder;

	@MockitoBean
	private LessonCompleter lessonCompleter;

	@Test
	void 교육_카테고리와_소속_토픽_목록을_반환한다() throws Exception {
		given(trainingCategoryFinder.findAllWithTopics()).willReturn(List.of(
				category(1L, "기본 교육", "기본기를 배우는 교육", "https://example.com/basic.png", 1,
						List.of(
								topic(10L, "앉아", "앉아 자세를 배우는 훈련", "차분히 앉는 방법을 익혀요",
										"https://example.com/sit.png", 1),
								topic(11L, "기다려", "기다림을 배우는 훈련", "보호자의 신호를 기다려요",
										"https://example.com/wait.png", 2)
						)
				),
				category(2L, "심화 교육", "응용 행동을 배우는 교육", "https://example.com/advanced.png", 2,
						List.of(
								topic(20L, "이리와", "호출에 반응하는 훈련", "보호자에게 바로 돌아와요",
										"https://example.com/come.png", 1)
						)
				)
		));

		mockMvc.perform(get("/api/training/categories")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryId").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryTitle").value("기본 교육"))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryDescription").value("기본기를 배우는 교육"))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryIconUrl")
						.value("https://example.com/basic.png"))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategorySortOrder").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicId").value(10))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicTitle").value("앉아"))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicDescription").value("앉아 자세를 배우는 훈련"))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicDetail").value("차분히 앉는 방법을 익혀요"))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicIconUrl")
						.value("https://example.com/sit.png"))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicSortOrder").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].topics[1].topicId").value(11))
				.andExpect(jsonPath("$.trainingCategories[1].trainingCategoryId").value(2))
				.andExpect(jsonPath("$.trainingCategories[1].topics[0].topicId").value(20))
				.andDo(document("training/categories",
						responseFields(
								fieldWithPath("trainingCategories[]").description("교육 카테고리 목록"),
								fieldWithPath("trainingCategories[].trainingCategoryId").description("교육 카테고리 ID"),
								fieldWithPath("trainingCategories[].trainingCategoryTitle").description("교육 카테고리 이름"),
								fieldWithPath("trainingCategories[].trainingCategoryDescription")
										.description("교육 카테고리 설명"),
								fieldWithPath("trainingCategories[].trainingCategoryIconUrl")
										.description("교육 카테고리 아이콘 URL"),
								fieldWithPath("trainingCategories[].trainingCategorySortOrder")
										.description("교육 카테고리 노출 순서. 오름차순 정렬"),
								fieldWithPath("trainingCategories[].topics[]").description("카테고리에 속한 토픽 목록. 없으면 빈 배열"),
								fieldWithPath("trainingCategories[].topics[].topicId").description("토픽 ID"),
								fieldWithPath("trainingCategories[].topics[].topicTitle").description("토픽 이름"),
								fieldWithPath("trainingCategories[].topics[].topicDescription").description("토픽 설명"),
								fieldWithPath("trainingCategories[].topics[].topicDetail").description("토픽 상세 설명"),
								fieldWithPath("trainingCategories[].topics[].topicIconUrl").description("토픽 아이콘 URL"),
								fieldWithPath("trainingCategories[].topics[].topicSortOrder")
										.description("토픽 노출 순서. 오름차순 정렬")
						)
				));
	}

	@Test
	void 토픽이_없는_카테고리는_빈_배열을_반환한다() throws Exception {
		given(trainingCategoryFinder.findAllWithTopics()).willReturn(List.of(
				category(1L, "기본 교육", "기본기를 배우는 교육", "https://example.com/basic.png", 1, List.of())
		));

		mockMvc.perform(get("/api/training/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories[0].topics").isArray())
				.andExpect(jsonPath("$.trainingCategories[0].topics").isEmpty());
	}

	@Test
	void 등록된_카테고리가_없으면_빈_배열과_200을_반환한다() throws Exception {
		given(trainingCategoryFinder.findAllWithTopics()).willReturn(List.of());

		mockMvc.perform(get("/api/training/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories").isArray())
				.andExpect(jsonPath("$.trainingCategories").isEmpty());
	}

	@Test
	void 선택한_토픽_ID를_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1))
				.andDo(document("training/topic-select",
						requestFields(
								fieldWithPath("topicId").description("필수 입력. 커리큘럼 화면에 표시할 토픽 ID")
						),
						responseFields(
								fieldWithPath("topicId").description("선택된 토픽 ID")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_토픽_선택을_위임한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(7L)))
				.andExpect(status().isOk());

		then(topicSelector).should().selectTopic(42L, 7L);
	}

	@Test
	void 같은_토픽을_연속으로_선택해도_200을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1));

		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(1L)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1));
	}

	@Test
	void 토픽_ID가_없으면_검증에_실패한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("topicId"));
	}

	@Test
	void 존재하지_않는_토픽이면_404와_에러_코드를_반환한다() throws Exception {
		willThrow(new TopicNotFoundException(999L)).given(topicSelector).selectTopic(42L, 999L);

		mockMvc.perform(put("/api/training/topic/selection")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(999L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 토픽을 찾을 수 없습니다."))
				.andDo(document("training/topic-select-error",
						requestFields(
								fieldWithPath("topicId").description("필수 입력. 커리큘럼 화면에 표시할 토픽 ID")
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
	void 토픽_선택_시_인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(1L)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

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
	void 커리큘럼_조회_시_인증_정보가_없으면_401을_반환한다() throws Exception {
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

	@Test
	void 레슨의_카드와_미디어_목록을_반환한다() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of(
				new CardResult(10L, "앉아 준비", 1, "간식을 손에 쥐고 앉아를 말하세요", List.of(
						new CardMediaResult(100L, 10L, IMAGE, "https://cdn.example.com/1.png", 1),
						new CardMediaResult(101L, 10L, VIDEO, "https://cdn.example.com/1.mp4", 2)
				)),
				new CardResult(11L, "앉아 보상", 2, "앉으면 바로 간식을 주세요", List.of(
						new CardMediaResult(102L, 11L, IMAGE, "https://cdn.example.com/2.png", 1)
				))
		));

		mockMvc.perform(get("/api/training/lessons/{lessonId}/cards", 1L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards[0].cardId").value(10))
				.andExpect(jsonPath("$.cards[0].cardTitle").value("앉아 준비"))
				.andExpect(jsonPath("$.cards[0].cardSortOrder").value(1))
				.andExpect(jsonPath("$.cards[0].instruction").value("간식을 손에 쥐고 앉아를 말하세요"))
				.andExpect(jsonPath("$.cards[0].cardMedia[0].cardMediaId").value(100))
				.andExpect(jsonPath("$.cards[0].cardMedia[0].cardId").value(10))
				.andExpect(jsonPath("$.cards[0].cardMedia[0].mediaType").value("IMAGE"))
				.andExpect(jsonPath("$.cards[0].cardMedia[0].url").value("https://cdn.example.com/1.png"))
				.andExpect(jsonPath("$.cards[0].cardMedia[0].sortOrder").value(1))
				.andExpect(jsonPath("$.cards[0].cardMedia[1].mediaType").value("VIDEO"))
				.andExpect(jsonPath("$.cards[1].cardId").value(11))
				.andExpect(jsonPath("$.cards[1].cardSortOrder").value(2))
				.andExpect(jsonPath("$.cards[1].cardMedia[0].cardMediaId").value(102))
				.andDo(document("training/lesson-cards",
						pathParameters(
								parameterWithName("lessonId").description("레슨 ID")
						),
						responseFields(
								fieldWithPath("cards[]").description("레슨의 카드 목록. 페이지네이션 없이 전부 내려간다"),
								fieldWithPath("cards[].cardId").description("카드 ID"),
								fieldWithPath("cards[].cardTitle").description("카드 타이틀. 없으면 빈 문자열"),
								fieldWithPath("cards[].cardSortOrder").description("카드 노출 순서. 오름차순 정렬"),
								fieldWithPath("cards[].instruction").description("카드 지시문. 없으면 빈 문자열"),
								fieldWithPath("cards[].cardMedia[]").description("카드에 속한 미디어 목록. 없으면 빈 배열"),
								fieldWithPath("cards[].cardMedia[].cardMediaId").description("카드 미디어 ID"),
								fieldWithPath("cards[].cardMedia[].cardId").description("미디어가 속한 카드 ID"),
								fieldWithPath("cards[].cardMedia[].mediaType").description("미디어 유형. `IMAGE` 또는 `VIDEO`"),
								fieldWithPath("cards[].cardMedia[].url").description("미디어 URL"),
								fieldWithPath("cards[].cardMedia[].sortOrder").description("미디어 노출 순서. 오름차순 정렬")
						)
				));
	}

	@Test
	void 미디어가_없는_카드는_빈_배열을_반환한다() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of(
				new CardResult(10L, "앉아 준비", 1, "간식을 손에 쥐고 앉아를 말하세요", List.of())
		));

		mockMvc.perform(get("/api/training/lessons/{lessonId}/cards", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards[0].cardMedia").isArray())
				.andExpect(jsonPath("$.cards[0].cardMedia").isEmpty());
	}

	@Test
	void 카드가_없는_레슨은_빈_배열과_200을_반환한다() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of());

		mockMvc.perform(get("/api/training/lessons/{lessonId}/cards", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards").isArray())
				.andExpect(jsonPath("$.cards").isEmpty());
	}

	@Test
	void 존재하지_않는_레슨이면_404와_에러_코드를_반환한다() throws Exception {
		given(lessonFinder.findCards(999L)).willThrow(new LessonNotFoundException(999L));

		mockMvc.perform(get("/api/training/lessons/{lessonId}/cards", 999L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_LESSON_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 레슨을 찾을 수 없습니다."))
				.andDo(document("training/lesson-cards-error",
						pathParameters(
								parameterWithName("lessonId").description("레슨 ID")
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
	void 레슨을_완료하면_레슨_ID와_갱신된_완료_횟수를_반환한다() throws Exception {
		given(lessonCompleter.completeLesson(42L, 1L)).willReturn(3);

		mockMvc.perform(post("/api/training/lessons/{lessonId}/completion", 1L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lessonId").value(1))
				.andExpect(jsonPath("$.completedCount").value(3))
				.andDo(document("training/lesson-complete",
						pathParameters(
								parameterWithName("lessonId").description("완료한 레슨 ID")
						),
						responseFields(
								fieldWithPath("lessonId").description("완료한 레슨 ID"),
								fieldWithPath("completedCount").description("증가가 반영된 반복 완료 횟수")
						)
				));
	}

	@Test
	void 인증_주체에서_읽은_사용자로_레슨_완료를_위임한다() throws Exception {
		mockMvc.perform(post("/api/training/lessons/{lessonId}/completion", 7L).principal(CURRENT_USER))
				.andExpect(status().isCreated());

		then(lessonCompleter).should().completeLesson(42L, 7L);
	}

	@Test
	void 존재하지_않는_레슨을_완료하면_404와_에러_코드를_반환한다() throws Exception {
		given(lessonCompleter.completeLesson(42L, 999L)).willThrow(new LessonNotFoundException(999L));

		mockMvc.perform(post("/api/training/lessons/{lessonId}/completion", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_LESSON_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 레슨을 찾을 수 없습니다."))
				.andDo(document("training/lesson-complete-error",
						pathParameters(
								parameterWithName("lessonId").description("완료한 레슨 ID")
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
	void 레슨_완료_시_인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/training/lessons/{lessonId}/completion", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private TrainingCategory category(Long id, String title, String description, String iconUrl, int sortOrder,
			List<Topic> topics) {
		TrainingCategory category = TrainingCategoryFixture.create(title, sortOrder, description, iconUrl);
		ReflectionTestUtils.setField(category, "id", id);
		ReflectionTestUtils.setField(category, "topics", topics);
		return category;
	}

	private Topic topic(Long id, String title, String description, String detail, String iconUrl, int sortOrder) {
		Topic topic = TopicFixture.create(null, title, sortOrder, description, detail, iconUrl);
		ReflectionTestUtils.setField(topic, "id", id);
		return topic;
	}

	private String selectionBody(long topicId) {
		return "{\"topicId\": " + topicId + "}";
	}
}
