package com.daesabu.meongcoach.training.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CardMediaResult;
import com.daesabu.meongcoach.training.application.provided.CardResult;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.domain.MediaType;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
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
 * 레슨 카드 조회·완료 API 검증.
 */
@WebMvcTest(TrainingLessonController.class)
@AutoConfigureRestDocs
class TrainingLessonControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LessonFinder lessonFinder;

	@MockitoBean
	private LessonCompleter lessonCompleter;

	@Test
	void 레슨의_카드와_미디어_목록을_반환한다() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of(
				new CardResult(10L, "앉아 준비", 1, "간식을 손에 쥐고 앉아를 말하세요", List.of(
						new CardMediaResult(100L, 10L, MediaType.IMAGE, "https://cdn.example.com/1.png", 1),
						new CardMediaResult(101L, 10L, MediaType.VIDEO, "https://cdn.example.com/1.mp4", 2)
				)),
				new CardResult(11L, "앉아 보상", 2, "앉으면 바로 간식을 주세요", List.of(
						new CardMediaResult(102L, 11L, MediaType.IMAGE, "https://cdn.example.com/2.png", 1)
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
}
