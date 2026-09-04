package com.daesabu.meongcoach.training.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 커리큘럼 화면 변경 API 검증.
 */
@WebMvcTest(TrainingTopicController.class)
@AutoConfigureRestDocs
class TrainingTopicControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TopicSelector topicSelector;

	private static String selectionBody(long topicId) {
		return "{\"topicId\": " + topicId + "}";
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
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(put("/api/training/topic/selection")
						.contentType(MediaType.APPLICATION_JSON)
						.content(selectionBody(1L)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
