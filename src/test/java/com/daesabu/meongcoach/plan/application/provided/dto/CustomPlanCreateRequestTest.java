package com.daesabu.meongcoach.plan.application.provided.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class CustomPlanCreateRequestTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void 토픽을_한_개부터_세_개까지_선택할_수_있다() {
		for (int count = 1; count <= 3; count++) {
			List<Long> topicIds = List.of(1L, 2L, 3L).subList(0, count);
			CustomPlanCreateRequest request = new CustomPlanCreateRequest(topicIds, 10, 3);

			assertThat(validator.validate(request)).isEmpty();
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	void 토픽_목록이_없으면_거절한다(List<Long> topicIds) {
		CustomPlanCreateRequest request = new CustomPlanCreateRequest(topicIds, 10, 3);

		assertThat(validator.validate(request)).anySatisfy(violation ->
				assertThat(violation.getPropertyPath().toString()).isEqualTo("topicIds"));
	}

	@Test
	void 토픽이_네_개면_거절한다() {
		CustomPlanCreateRequest request = new CustomPlanCreateRequest(List.of(1L, 2L, 3L, 4L), 10, 3);

		assertThat(validator.validate(request)).anySatisfy(violation ->
				assertThat(violation.getPropertyPath().toString()).isEqualTo("topicIds"));
	}

	@Test
	void 토픽_목록에_null이_있으면_거절한다() {
		CustomPlanCreateRequest request = new CustomPlanCreateRequest(Arrays.asList(1L, null), 10, 3);

		assertThat(validator.validate(request)).anySatisfy(violation ->
				assertThat(violation.getPropertyPath().toString()).startsWith("topicIds[1]"));
	}

	@Test
	void 실천_약속의_필수_입력이_누락되면_거절한다() {
		CustomPlanCreateRequest request = new CustomPlanCreateRequest(List.of(1L), null, null);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("dailyMinutes", "daysPerWeek");
	}
}
