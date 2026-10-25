package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerateRequest;
import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.exception.InvalidAiPlanResponseException;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AiPlanGenerationAdapterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final AiContentGenerator contentGenerator = mock(AiContentGenerator.class);
	private final AiPlanPromptBuilder promptBuilder = new AiPlanPromptBuilder(objectMapper, "{{input}}");
	private final String systemPrompt = "제공된 후보에서 필요한 토픽을 선택한다.";
	private final String responseSchema = "{\"type\":\"object\"}";
	private final AiPlanGenerationAdapter adapter = new AiPlanGenerationAdapter(
			promptBuilder, contentGenerator, systemPrompt, responseSchema);

	@Test
	void 프롬프트와_스키마를_AI_모듈에_전달하고_생성된_문자열을_반환한다() {
		AiPlanGenerateRequest request = createRequest();
		String generatedContent = "{\"topicIds\":[1],\"goal\":\"차분하게 기다려요\"}";
		given(contentGenerator.generate(any())).willReturn(generatedContent);

		String result = adapter.generateContent(request);

		ArgumentCaptor<AiContentGenerateRequest> captor = ArgumentCaptor.forClass(AiContentGenerateRequest.class);
		verify(contentGenerator).generate(captor.capture());
		AiContentGenerateRequest contentRequest = captor.getValue();
		assertThat(contentRequest.systemPrompt()).isEqualTo(systemPrompt);
		assertThat(contentRequest.responseSchema()).isEqualTo(responseSchema);
		JsonNode input = objectMapper.readTree(contentRequest.userPrompt());
		assertThat(input.at("/dog/breed").asString()).isEqualTo("푸들");
		assertThat(input.get("expectation").asString()).isEqualTo(request.expectation());
		assertThat(result).isEqualTo(generatedContent);
	}

	@Test
	void AI_모듈의_호출_실패를_그대로_전달한다() {
		AiPlanGenerateRequest request = createRequest();
		RuntimeException failure = new RuntimeException("AI 호출 실패");
		given(contentGenerator.generate(any())).willThrow(failure);

		assertThatThrownBy(() -> adapter.generateContent(request)).isSameAs(failure);
		verify(contentGenerator).generate(any());
	}

	@Test
	void 후보에_있는_서로_다른_토픽과_목표는_허용한다() {
		AiPlanGenerateRequest request = createRequest();
		AiPlanGenerateResult result = new AiPlanGenerateResult(List.of(2L, 1L), "차분히 기다리고 다가와요");

		assertThatCode(() -> adapter.validateResult(result, request)).doesNotThrowAnyException();
		assertThat(result.topicIds()).containsExactly(2L, 1L);
	}

	@ParameterizedTest
	@MethodSource("invalidResults")
	void 사용할_수_없는_AI_결과를_거절한다(AiPlanGenerateResult result) {
		AiPlanGenerateRequest request = createRequest();

		assertThatThrownBy(() -> adapter.validateResult(result, request))
				.isInstanceOf(InvalidAiPlanResponseException.class)
				.satisfies(exception -> {
					InvalidAiPlanResponseException failure = (InvalidAiPlanResponseException) exception;
					assertThat(failure.getErrorCode().code()).isEqualTo("PLAN_AI_INVALID_RESPONSE");
					assertThat(failure.getErrorCode().status()).isEqualTo(502);
				});
	}

	private static Stream<AiPlanGenerateResult> invalidResults() {
		return Stream.of(
				null,
				new AiPlanGenerateResult(null, "목표"),
				new AiPlanGenerateResult(List.of(), "목표"),
				new AiPlanGenerateResult(List.of(1L), null),
				new AiPlanGenerateResult(List.of(1L), ""),
				new AiPlanGenerateResult(List.of(1L), " \n\t"),
				new AiPlanGenerateResult(Arrays.asList(1L, null), "목표"),
				new AiPlanGenerateResult(List.of(1L, 1L), "목표"),
				new AiPlanGenerateResult(List.of(999L), "목표")
		);
	}

	private AiPlanGenerateRequest createRequest() {
		DogProfileResult dog = new DogProfileResult(1L, "초코", new Breed("푸들"),
				LocalDate.of(2025, 1, 31), null);
		List<AiPlanCurriculumResult> curriculums = List.of(
				new AiPlanCurriculumResult(10L, 1L, "기다리기", "짧게 기다리기", "기다리는 연습", 2, 0, 10),
				new AiPlanCurriculumResult(11L, 1L, "기다리기", "길게 기다리기", "기다리는 연습", 2, 0, 15),
				new AiPlanCurriculumResult(20L, 2L, "이리 와", "다가오기", "다가오는 연습", 2, 0, 20));
		return new AiPlanGenerateRequest(dog, LocalDate.of(2026, 10, 26), 10, 3, "차분하게 기다리기", curriculums);
	}
}
