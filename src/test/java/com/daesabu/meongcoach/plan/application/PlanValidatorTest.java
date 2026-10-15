package com.daesabu.meongcoach.plan.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.exception.PlanExpectationRequiredException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicsRequiredException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanValidatorTest {

	private final PlanValidator validator = new PlanValidator();

	@Test
	void AI는_기대사항이_없으면_거절한다() {
		PlanCreateRequest request = request("AI", null, null);

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_5자_미만이면_거절한다() {
		PlanCreateRequest request = request("AI", null, "  짖음  ");

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_200자를_넘으면_거절한다() {
		PlanCreateRequest request = request("AI", null, "가".repeat(201));

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_5자_이상_200자_이하면_통과한다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, " 짖음 줄이기 ")))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, "가".repeat(5))))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, "가".repeat(200))))
				.doesNotThrowAnyException();
	}

	@Test
	void AI는_토픽을_보내도_기대사항만_본다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", List.of(1L), "짖음 줄이기")))
				.doesNotThrowAnyException();
	}

	@Test
	void MANUAL은_토픽이_없으면_거절한다() {
		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", null, null)))
				.isInstanceOf(PlanTopicsRequiredException.class);
		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(), null)))
				.isInstanceOf(PlanTopicsRequiredException.class);
	}

	@Test
	void MANUAL은_토픽이_3개를_넘으면_거절한다() {
		PlanCreateRequest request = request("MANUAL", List.of(1L, 2L, 3L, 4L), null);

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request))
				.isInstanceOf(PlanTopicsRequiredException.class);
	}

	@Test
	void MANUAL은_토픽이_1개_이상_3개_이하면_기대사항_없이_통과한다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(1L), null)))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(1L, 2L, 3L), null)))
				.doesNotThrowAnyException();
	}

	private static PlanCreateRequest request(String mode, List<Long> topicIds, String expectation) {
		return new PlanCreateRequest(2L, mode, topicIds, expectation, 10, 3, false);
	}
}
