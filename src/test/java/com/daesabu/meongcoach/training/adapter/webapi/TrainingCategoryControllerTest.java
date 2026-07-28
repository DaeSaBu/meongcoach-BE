package com.daesabu.meongcoach.training.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.training.application.provided.TopicView;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 라이브러리 탭 진입 API 검증.
 */
@WebMvcTest(TrainingCategoryController.class)
@AutoConfigureRestDocs
@DisplayName("라이브러리 탭 진입 API")
class TrainingCategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TrainingCategoryFinder trainingCategoryFinder;

	@Test
	@DisplayName("교육 카테고리와 소속 토픽 목록을 반환한다")
	void findAllReturnsCategoriesWithTopics() throws Exception {
		given(trainingCategoryFinder.findAll()).willReturn(List.of(
				new TrainingCategoryView(1L, "기본 교육", 1, List.of(
						new TopicView(10L, "앉아", 1),
						new TopicView(11L, "기다려", 2)
				)),
				new TrainingCategoryView(2L, "심화 교육", 2, List.of(
						new TopicView(20L, "이리와", 1)
				))
		));

		mockMvc.perform(get("/api/training/training-categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryId").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategoryTitle").value("기본 교육"))
				.andExpect(jsonPath("$.trainingCategories[0].trainingCategorySortOrder").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicId").value(10))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicTitle").value("앉아"))
				.andExpect(jsonPath("$.trainingCategories[0].topics[0].topicSortOrder").value(1))
				.andExpect(jsonPath("$.trainingCategories[0].topics[1].topicId").value(11))
				.andExpect(jsonPath("$.trainingCategories[1].trainingCategoryId").value(2))
				.andExpect(jsonPath("$.trainingCategories[1].topics[0].topicId").value(20))
				.andDo(document("training/training-categories",
						responseFields(
								fieldWithPath("trainingCategories[]").description("교육 카테고리 목록"),
								fieldWithPath("trainingCategories[].trainingCategoryId").description("교육 카테고리 ID"),
								fieldWithPath("trainingCategories[].trainingCategoryTitle").description("교육 카테고리 이름"),
								fieldWithPath("trainingCategories[].trainingCategorySortOrder")
										.description("교육 카테고리 노출 순서. 오름차순 정렬"),
								fieldWithPath("trainingCategories[].topics[]").description("카테고리에 속한 토픽 목록. 없으면 빈 배열"),
								fieldWithPath("trainingCategories[].topics[].topicId").description("토픽 ID"),
								fieldWithPath("trainingCategories[].topics[].topicTitle").description("토픽 이름"),
								fieldWithPath("trainingCategories[].topics[].topicSortOrder")
										.description("토픽 노출 순서. 오름차순 정렬")
						)
				));
	}

	@Test
	@DisplayName("토픽이 없는 카테고리는 빈 배열을 반환한다")
	void findAllReturnsEmptyTopicsWhenCategoryHasNoTopic() throws Exception {
		given(trainingCategoryFinder.findAll()).willReturn(List.of(
				new TrainingCategoryView(1L, "기본 교육", 1, List.of())
		));

		mockMvc.perform(get("/api/training/training-categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories[0].topics").isArray())
				.andExpect(jsonPath("$.trainingCategories[0].topics").isEmpty());
	}

	@Test
	@DisplayName("등록된 카테고리가 없으면 빈 배열과 200을 반환한다")
	void findAllReturnsEmptyArrayWhenNoCategoryExists() throws Exception {
		given(trainingCategoryFinder.findAll()).willReturn(List.of());

		mockMvc.perform(get("/api/training/training-categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.trainingCategories").isArray())
				.andExpect(jsonPath("$.trainingCategories").isEmpty());
	}
}
