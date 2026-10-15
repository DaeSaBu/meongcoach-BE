package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanCatalogEmptyException;
import com.daesabu.meongcoach.plan.domain.exception.PlanExpectationRequiredException;
import com.daesabu.meongcoach.plan.domain.exception.PlanInvalidModeException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicNotFoundException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicsRequiredException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanCreatorTest extends PlanApplicationTestSupport {

	@Autowired
	private PlanCreator planCreator;

	@Autowired
	private StructuredChatClient structuredChatClient;

	@Test
	void 직접_고르기는_고른_토픽으로_플랜을_저장하고_상세를_반환한다() {
		PlanDetailResult result = planCreator.create(userId, manualRequest(false, sitTopicId));

		assertThat(result.mode()).isEqualTo(PlanMode.MANUAL);
		assertThat(result.generator()).isEqualTo(PlanGeneratorType.MANUAL);
		assertThat(result.dogName()).isEqualTo("초코");
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.weeks()).hasSize(2);
		assertThat(result.weeks().getFirst().items()).extracting(PlanDetailResult.Item::curriculumId)
				.containsExactly(sitBasicId);
		assertThat(result.today().curriculumId()).isEqualTo(sitBasicId);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
	}

	@Test
	void 직접_고르기는_요청을_인터뷰_JSON으로_저장하고_기대사항을_바꾸지_않는다() {
		planCreator.create(userId, manualRequest(false, sitTopicId, comeTopicId));

		TrainingPlan saved = plansOf(PlanStatus.ACTIVE).getFirst();
		assertThat(saved.getInterviewJson()).contains("\"mode\":\"MANUAL\"")
				.contains("\"dailyMinutes\":10")
				.contains("\"daysPerWeek\":3");
		assertThat(dogRepository.findById(dogId).orElseThrow().getExpectation()).isEqualTo(ORIGINAL_EXPECTATION);
	}

	@Test
	void AI_모드는_대체_플랜을_저장하고_강아지_기대사항을_갱신한다() {
		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.mode()).isEqualTo(PlanMode.AI);
		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
		assertThat(plansOf(PlanStatus.ACTIVE).getFirst().getInterviewJson()).contains(NEW_EXPECTATION);
		assertThat(dogRepository.findById(dogId).orElseThrow().getExpectation()).isEqualTo(NEW_EXPECTATION);
		then(structuredChatClient).shouldHaveNoInteractions();
	}

	@Test
	void 진행_중_플랜이_있고_교체하지_않으면_충돌하며_아무것도_저장하지_않는다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));

		assertThatThrownBy(() -> planCreator.create(userId, aiRequest(NEW_EXPECTATION, false)))
				.isInstanceOf(PlanAlreadyActiveException.class);

		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
		assertThat(plansOf(PlanStatus.ABANDONED)).isEmpty();
		assertThat(dogRepository.findById(dogId).orElseThrow().getExpectation()).isEqualTo(ORIGINAL_EXPECTATION);
		then(structuredChatClient).shouldHaveNoInteractions();
	}

	@Test
	void 교체하면_기존_플랜은_포기_상태가_되고_새_플랜이_진행된다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));

		planCreator.create(userId, manualRequest(true, comeTopicId));

		assertThat(plansOf(PlanStatus.ABANDONED)).hasSize(1);
		List<TrainingPlan> active = plansOf(PlanStatus.ACTIVE);
		assertThat(active).hasSize(1);
		assertThat(active.getFirst().curriculumIds()).containsExactly(comeBasicId);
	}

	@Test
	void 잠긴_토픽만_고르면_충돌한다() {
		assertThatThrownBy(() -> planCreator.create(userId, manualRequest(false, lockedTopicId)))
				.isInstanceOf(PlanCatalogEmptyException.class);

		assertThat(plansOf(PlanStatus.ACTIVE)).isEmpty();
	}

	@Test
	void 없는_토픽을_고르면_찾을_수_없다() {
		assertThatThrownBy(() -> planCreator.create(userId, manualRequest(false, 123456L)))
				.isInstanceOf(PlanTopicNotFoundException.class);
	}

	@Test
	void AI_모드에_기대사항이_없으면_거절한다() {
		assertThatThrownBy(() -> planCreator.create(userId, aiRequest(null, false)))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void 직접_고르기에_토픽이_없으면_거절한다() {
		assertThatThrownBy(() -> planCreator.create(userId, manualRequest(false)))
				.isInstanceOf(PlanTopicsRequiredException.class);
	}

	@Test
	void 알_수_없는_방식이면_거절한다() {
		PlanCreateRequest request = new PlanCreateRequest(dogId, "UNKNOWN", null, null, 10, 3, false);

		assertThatThrownBy(() -> planCreator.create(userId, request))
				.isInstanceOf(PlanInvalidModeException.class);
	}

	@Test
	void 하루_분량이_범위를_벗어나면_검증에_실패한다() {
		PlanCreateRequest request = new PlanCreateRequest(dogId, "MANUAL", List.of(sitTopicId), null, 0, 3, false);

		assertThatThrownBy(() -> planCreator.create(userId, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 다른_사용자의_강아지로는_만들_수_없다() {
		assertThatThrownBy(() -> planCreator.create(userId + 1000, manualRequest(false, sitTopicId)))
				.isInstanceOf(DogNotFoundException.class);
	}
}
