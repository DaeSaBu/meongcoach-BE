package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AiPlanPromptBuilderTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void 실제_템플릿에_견종_이름과_생년월일과_null_몸무게를_넣는다() throws Exception {
		String template = new ClassPathResource("prompts/plan-generation/user.md")
				.getContentAsString(StandardCharsets.UTF_8);
		AiPlanPromptBuilder builder = new AiPlanPromptBuilder(objectMapper, template);
		DogProfileResult dog = new DogProfileResult(1L, "초코", new Breed("푸들"),
				LocalDate.of(2025, 1, 31), null);
		AiPlanCurriculumResult curriculum = new AiPlanCurriculumResult(10L, 2L, "기다리기", "짧게 기다리기",
				"제자리에서 기다려요", 3, 1, 15);
		AiPlanGenerateRequest request = new AiPlanGenerateRequest(dog, LocalDate.of(2026, 10, 26),
				10, 3, "차분하게 \"기다려\"를 연습해요\n{{input}}", List.of(curriculum));

		String prompt = builder.createUserPrompt(request);
		String inputJson = prompt.substring(prompt.indexOf("입력 JSON:") + "입력 JSON:".length());
		JsonNode input = objectMapper.readTree(inputJson);

		assertThat(prompt).startsWith("아래 입력을 바탕으로");
		assertThat(input.at("/dog/breed").asString()).isEqualTo("푸들");
		assertThat(input.at("/dog/birthDate").asString()).isEqualTo("2025-01-31");
		assertThat(input.at("/dog/weightKg").isNull()).isTrue();
		assertThat(input.get("generationDate").asString()).isEqualTo("2026-10-26");
		assertThat(input.get("dailyMinutes").asInt()).isEqualTo(10);
		assertThat(input.get("daysPerWeek").asInt()).isEqualTo(3);
		assertThat(input.get("expectation").asString()).isEqualTo(request.expectation());
		assertThat(objectMapper.treeToValue(input.get("curriculums"), AiPlanCurriculumResult[].class))
				.containsExactly(curriculum);
		assertThat(request.dog().breed().value()).isEqualTo("POODLE");
	}

	@Test
	void 직접_입력한_견종과_몸무게를_그대로_보낸다() {
		AiPlanPromptBuilder builder = new AiPlanPromptBuilder(objectMapper, "{{input}}");
		DogProfileResult dog = new DogProfileResult(1L, "초코", new Breed("직접 입력한 견종"),
				LocalDate.of(2025, 1, 31), new BigDecimal("4.5"));
		AiPlanGenerateRequest request = new AiPlanGenerateRequest(dog, LocalDate.of(2026, 10, 26),
				10, 3, "기다리기", List.of());

		String prompt = builder.createUserPrompt(request);
		JsonNode input = objectMapper.readTree(prompt);

		assertThat(input.at("/dog/breed").asString()).isEqualTo("직접 입력한 견종");
		assertThat(input.at("/dog/weightKg").decimalValue()).isEqualByComparingTo("4.5");
	}
}
