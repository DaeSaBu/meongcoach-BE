package com.daesabu.meongcoach.home.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@AutoConfigureRestDocs
class HomeControllerTest {

	private static final Principal CURRENT_USER = () -> "42";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuickTopicFinder quickTopicFinder;

	@Test
	void 강아지에게_보여_줄_바로_훈련_토픽을_반환한다() throws Exception {
		TopicProgressResult continued = new TopicProgressResult(1102L, "이리와", "부르면 달려와요", "https://cdn.test/icon.png",
				"https://cdn.test/1102.jpg", true, 2L, "주니어 교육", 3, 6, 3, ProgressStatus.IN_PROGRESS, 110202L, LocalDateTime.of(2026, 11, 7, 21, 30));
		TopicProgressResult fresh = new TopicProgressResult(1303L, "사람 사회화", "낯선 사람과 친해져요", "https://cdn.test/icon.png",
				null, false, 15L, "퍼피 교육", 2, 4, 0, ProgressStatus.NOT_STARTED, 130301L, null);
		given(quickTopicFinder.find(42L, 7L)).willReturn(List.of(new QuickTopicResult(continued, QuickTopicRole.IN_PROGRESS),
				new QuickTopicResult(fresh, QuickTopicRole.NOT_STARTED)));

		mockMvc.perform(get("/api/home/quick-topics").queryParam("dogId", "7").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topics[0].topicId").value(1102))
				.andExpect(jsonPath("$.topics[0].role").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.topics[0].nextCurriculumId").value(110202))
				.andExpect(jsonPath("$.topics[1].thumbnailUrl").value(nullValue()))
				.andExpect(jsonPath("$.topics[1].role").value("NOT_STARTED"))
				.andDo(document("home/quick-topics",
						queryParameters(parameterWithName("dogId").description("본인 소유 강아지 ID")),
						responseFields(
								fieldWithPath("topics").description("보여 줄 순서의 토픽 목록, 최대 5개"),
								fieldWithPath("topics[].topicId").description("토픽 ID"),
								fieldWithPath("topics[].title").description("토픽 제목"),
								fieldWithPath("topics[].description").description("토픽 설명"),
								fieldWithPath("topics[].categoryTitle").description("토픽이 속한 카테고리 제목"),
								fieldWithPath("topics[].iconUrl").description("토픽 아이콘 URL"),
								fieldWithPath("topics[].thumbnailUrl").type(JsonFieldType.STRING).optional()
										.description("카드 전면 사진 URL. 등록 전이면 null"),
								fieldWithPath("topics[].role").description(
										"IN_PROGRESS(진행 중인 토픽, 첫 칸 이어서), NOT_STARTED(시작하지 않은 토픽, 새로 도전)"),
								fieldWithPath("topics[].totalLessons").description("토픽 전체 레슨 수"),
								fieldWithPath("topics[].completedLessons").description("회원이 완료한 레슨 수"),
								fieldWithPath("topics[].nextCurriculumId").description(
										"카드를 누르면 열 커리큘럼 ID. 완료하지 않은 첫 커리큘럼, 모두 완료했으면 첫 커리큘럼"))));
	}

	@Test
	void 다른_회원의_강아지로_조회하면_404를_반환한다() throws Exception {
		given(quickTopicFinder.find(42L, 7L)).willThrow(new DogNotFoundException(7L));

		mockMvc.perform(get("/api/home/quick-topics").queryParam("dogId", "7").principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andDo(document("home/quick-topics-error",
						queryParameters(parameterWithName("dogId").description("본인 소유 강아지 ID")),
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("에러 설명"),
								fieldWithPath("instance").description("요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)"))));
	}
}
