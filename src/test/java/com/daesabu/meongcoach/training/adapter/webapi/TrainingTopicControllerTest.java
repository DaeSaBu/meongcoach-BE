package com.daesabu.meongcoach.training.adapter.webapi;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 커리큘럼 화면 변경 API 검증.
 */
@WebMvcTest(TrainingTopicController.class)
@AutoConfigureRestDocs
@DisplayName("커리큘럼 화면 변경 API")
class TrainingTopicControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TopicSelector topicSelector;

	@Test
	@DisplayName("선택한 토픽 ID를 반환한다")
	void selectTopicReturnsSelectedTopicId() throws Exception {
		mockMvc.perform(put("/api/training/topics/{topicId}", 1L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1))
				.andDo(document("training/topic-select",
						pathParameters(
								parameterWithName("topicId").description("커리큘럼 화면에 표시할 토픽 ID")
						),
						responseFields(
								fieldWithPath("topicId").description("선택된 토픽 ID")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 토픽 선택을 위임한다")
	void selectTopicDelegatesWithCurrentUserId() throws Exception {
		mockMvc.perform(put("/api/training/topics/{topicId}", 7L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(topicSelector).should().selectTopic(42L, 7L);
	}

	@Test
	@DisplayName("같은 토픽을 연속으로 선택해도 200을 반환한다")
	void selectTopicReturnsOkWhenSelectedRepeatedly() throws Exception {
		mockMvc.perform(put("/api/training/topics/{topicId}", 1L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1));

		mockMvc.perform(put("/api/training/topics/{topicId}", 1L).principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicId").value(1));
	}

	@Test
	@DisplayName("존재하지 않는 토픽이면 404와 에러 코드를 반환한다")
	void selectTopicReturnsNotFoundWhenTopicDoesNotExist() throws Exception {
		willThrow(new TopicNotFoundException(999L)).given(topicSelector).selectTopic(42L, 999L);

		mockMvc.perform(put("/api/training/topics/{topicId}", 999L).principal(CURRENT_USER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 토픽을 찾을 수 없습니다."))
				.andDo(document("training/topic-select-error",
						pathParameters(
								parameterWithName("topicId").description("커리큘럼 화면에 표시할 토픽 ID")
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
	@DisplayName("인증 정보가 없으면 401을 반환한다")
	void selectTopicReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(put("/api/training/topics/{topicId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
