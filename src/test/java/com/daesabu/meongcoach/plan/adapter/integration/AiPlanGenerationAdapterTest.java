package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
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

	private AiPlanGenerateRequest createRequest() {
		DogProfileResult dog = new DogProfileResult(1L, "초코", new Breed("푸들"),
				LocalDate.of(2025, 1, 31), null);
		return new AiPlanGenerateRequest(dog, LocalDate.of(2026, 10, 26), 10, 3, "차분하게 기다리기", List.of());
	}
}
