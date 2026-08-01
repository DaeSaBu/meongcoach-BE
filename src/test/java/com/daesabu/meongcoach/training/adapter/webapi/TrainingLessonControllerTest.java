package com.daesabu.meongcoach.training.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.CardMediaView;
import com.daesabu.meongcoach.training.application.provided.CardView;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.domain.MediaType;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 레슨 카드 조회·완료 API 검증.
 */
@WebMvcTest(TrainingLessonController.class)
@AutoConfigureRestDocs
@DisplayName("레슨 API")
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
	@DisplayName("레슨의 카드와 미디어 목록을 반환한다")
	void findCardsReturnsCardsWithCardMedia() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of(
				new CardView(10L, "앉아 준비", 1, "간식을 손에 쥐고 앉아를 말하세요", List.of(
						new CardMediaView(100L, 10L, MediaType.IMAGE, "https://cdn.example.com/1.png", 1),
						new CardMediaView(101L, 10L, MediaType.VIDEO, "https://cdn.example.com/1.mp4", 2)
				)),
				new CardView(11L, "앉아 보상", 2, "앉으면 바로 간식을 주세요", List.of(
						new CardMediaView(102L, 11L, MediaType.IMAGE, "https://cdn.example.com/2.png", 1)
				))
		));

		mockMvc.perform(get("/api/training/lessons/{lessonId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards[0].cardId").value(10))
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
	@DisplayName("미디어가 없는 카드는 빈 배열을 반환한다")
	void findCardsReturnsEmptyCardMediaWhenCardHasNoMedia() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of(
				new CardView(10L, "앉아 준비", 1, "간식을 손에 쥐고 앉아를 말하세요", List.of())
		));

		mockMvc.perform(get("/api/training/lessons/{lessonId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards[0].cardMedia").isArray())
				.andExpect(jsonPath("$.cards[0].cardMedia").isEmpty());
	}

	@Test
	@DisplayName("카드가 없는 레슨은 빈 배열과 200을 반환한다")
	void findCardsReturnsEmptyArrayWhenLessonHasNoCard() throws Exception {
		given(lessonFinder.findCards(1L)).willReturn(List.of());

		mockMvc.perform(get("/api/training/lessons/{lessonId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cards").isArray())
				.andExpect(jsonPath("$.cards").isEmpty());
	}

	@Test
	@DisplayName("존재하지 않는 레슨이면 404와 에러 코드를 반환한다")
	void findCardsReturnsNotFoundWhenLessonDoesNotExist() throws Exception {
		given(lessonFinder.findCards(999L)).willThrow(new LessonNotFoundException(999L));

		mockMvc.perform(get("/api/training/lessons/{lessonId}", 999L))
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
	@DisplayName("레슨을 완료하면 레슨 ID와 갱신된 완료 횟수를 반환한다")
	void completeLessonReturnsUpdatedCompletedCount() throws Exception {
		given(lessonCompleter.completeLesson(42L, 1L)).willReturn(3);

		mockMvc.perform(post("/api/training/lessons/{lessonId}", 1L).principal(CURRENT_USER))
				.andExpect(status().isOk())
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
	@DisplayName("인증 주체에서 읽은 사용자로 레슨 완료를 위임한다")
	void completeLessonDelegatesWithCurrentUserId() throws Exception {
		mockMvc.perform(post("/api/training/lessons/{lessonId}", 7L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(lessonCompleter).should().completeLesson(42L, 7L);
	}

	@Test
	@DisplayName("존재하지 않는 레슨을 완료하면 404와 에러 코드를 반환한다")
	void completeLessonReturnsNotFoundWhenLessonDoesNotExist() throws Exception {
		given(lessonCompleter.completeLesson(42L, 999L)).willThrow(new LessonNotFoundException(999L));

		mockMvc.perform(post("/api/training/lessons/{lessonId}", 999L).principal(CURRENT_USER))
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
	@DisplayName("레슨 완료 시 인증 정보가 없으면 401을 반환한다")
	void completeLessonReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(post("/api/training/lessons/{lessonId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
