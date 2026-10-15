package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import com.daesabu.meongcoach.ai.domain.exception.StructuredChatCompletionFailedException;
import com.daesabu.meongcoach.dog.application.provided.DogProfileDeleter;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
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
import com.daesabu.meongcoach.training.domain.Curriculum;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@ApplicationTest
class PlanCreatorTest extends PlanApplicationTestSupport {

	private static final Long UNKNOWN_CURRICULUM_ID = 987654321L;

	@Autowired
	private PlanCreator planCreator;

	@Autowired
	private DogProfileDeleter dogProfileDeleter;

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
	void AI_모드는_모델_초안대로_플랜을_저장하고_강아지_기대사항을_갱신한다() {
		given(structuredChatClient.complete(any())).willReturn(draftJson(List.of(sitBasicId), List.of(sitAdvancedId, comeBasicId)));

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.mode()).isEqualTo(PlanMode.AI);
		assertThat(result.generator()).isEqualTo(PlanGeneratorType.AI);
		assertThat(result.adjustedReason()).isNull();
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.title()).isEqualTo("초코의 2주 플랜");
		assertThat(result.weeks()).extracting(PlanDetailResult.Week::theme).containsExactly("1주 테마", "2주 테마");
		assertThat(result.weeks().get(0).items()).extracting(PlanDetailResult.Item::curriculumId)
				.containsExactly(sitBasicId);
		assertThat(result.weeks().get(1).items()).extracting(PlanDetailResult.Item::curriculumId)
				.containsExactly(sitAdvancedId, comeBasicId);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
		assertThat(plansOf(PlanStatus.ACTIVE).getFirst().getInterviewJson()).contains(NEW_EXPECTATION);
		assertThat(dogRepository.findById(dogId).orElseThrow().getExpectation()).isEqualTo(NEW_EXPECTATION);
		then(structuredChatClient).should(times(1)).complete(any());
	}

	@Test
	void AI_초안이_검증에_실패하면_위반_내용을_붙여_한_번_다시_요청한다() {
		given(structuredChatClient.complete(any())).willReturn(draftJson(List.of(sitBasicId), List.of(UNKNOWN_CURRICULUM_ID)),
				draftJson(List.of(sitBasicId), List.of(comeBasicId)));

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.generator()).isEqualTo(PlanGeneratorType.AI);
		assertThat(result.weeks().get(1).items()).extracting(PlanDetailResult.Item::curriculumId)
				.containsExactly(comeBasicId);
		ArgumentCaptor<StructuredChatRequest> captor = ArgumentCaptor.forClass(StructuredChatRequest.class);
		then(structuredChatClient).should(times(2)).complete(captor.capture());
		assertThat(captor.getAllValues().get(0).userPrompt()).doesNotContain("이전 응답에서 다음 문제가 있었습니다");
		assertThat(captor.getAllValues().get(1).userPrompt())
				.contains("이전 응답에서 다음 문제가 있었습니다")
				.contains("카탈로그에 없는 curriculumId입니다: " + UNKNOWN_CURRICULUM_ID);
	}

	@Test
	void AI_초안이_재시도에서도_검증에_실패하면_대체_플랜을_저장한다() {
		given(structuredChatClient.complete(any())).willReturn(draftJson(List.of(sitBasicId), List.of(UNKNOWN_CURRICULUM_ID)));

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.mode()).isEqualTo(PlanMode.AI);
		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
		assertThat(dogRepository.findById(dogId).orElseThrow().getExpectation()).isEqualTo(NEW_EXPECTATION);
		then(structuredChatClient).should(times(2)).complete(any());
	}

	@Test
	void 모델_호출이_실패하면_재시도_없이_대체_플랜을_저장한다() {
		given(structuredChatClient.complete(any())).willThrow(new StructuredChatCompletionFailedException("호출 실패"));

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
		then(structuredChatClient).should(times(1)).complete(any());
	}

	@Test
	void 재시도_호출이_실패하면_대체_플랜을_저장한다() {
		given(structuredChatClient.complete(any()))
				.willReturn(draftJson(List.of(sitBasicId), List.of(UNKNOWN_CURRICULUM_ID)))
				.willThrow(new StructuredChatCompletionFailedException("호출 실패"));

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		then(structuredChatClient).should(times(2)).complete(any());
	}

	@Test
	void 모델_응답이_JSON이_아니면_재시도_없이_대체_플랜을_저장한다() {
		given(structuredChatClient.complete(any())).willReturn("플랜을 만들었어요");

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		then(structuredChatClient).should(times(1)).complete(any());
	}

	@Test
	void 열린_커리큘럼이_2개_미만이면_모델을_부르지_않고_대체_플랜을_저장한다() {
		lockCurriculums(sitAdvancedId, comeBasicId);

		PlanDetailResult result = planCreator.create(userId, aiRequest(NEW_EXPECTATION, false));

		assertThat(result.generator()).isEqualTo(PlanGeneratorType.FALLBACK);
		assertThat(result.durationWeeks()).isEqualTo(1);
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
	void 진행_중_플랜의_강아지가_삭제됐으면_교체하지_않아도_포기하고_새_플랜을_만든다() {
		Long secondDogId = saveSecondDog();
		planCreator.create(userId, manualRequest(false, sitTopicId));
		dogProfileDeleter.delete(userId, dogId);
		entityManager.flush();
		entityManager.clear();

		PlanDetailResult result = planCreator.create(userId, manualRequestFor(secondDogId, false, comeTopicId));

		assertThat(result.dogId()).isEqualTo(secondDogId);
		assertThat(plansOf(PlanStatus.ABANDONED)).hasSize(1);
		assertThat(plansOf(PlanStatus.ACTIVE)).hasSize(1);
	}

	@Test
	void 진행_중_플랜의_강아지가_있으면_다른_강아지로도_교체하지_않으면_충돌한다() {
		Long secondDogId = saveSecondDog();
		planCreator.create(userId, manualRequest(false, sitTopicId));

		assertThatThrownBy(() -> planCreator.create(userId, manualRequestFor(secondDogId, false, comeTopicId)))
				.isInstanceOf(PlanAlreadyActiveException.class);
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

	private String draftJson(List<Long> firstWeekIds, List<Long> secondWeekIds) {
		return """
				{"title":"초코의 2주 플랜","goal":"차근차근 익혀요","reason":"겁이 많아 기초부터 짰어요",
				"weeks":[{"weekNo":1,"theme":"1주 테마","milestone":"1주 마일스톤","curriculumIds":%s},
				{"weekNo":2,"theme":"2주 테마","milestone":"2주 마일스톤","curriculumIds":%s}]}
				""".formatted(firstWeekIds, secondWeekIds);
	}

	private void lockCurriculums(Long... curriculumIds) {
		for (Long curriculumId : curriculumIds) {
			Curriculum curriculum = curriculumRepository.findById(curriculumId).orElseThrow();
			ReflectionTestUtils.setField(curriculum, "isPremium", true);
			Object category = Hibernate.unproxy(curriculum.getTopic().getTrainingCategory());
			ReflectionTestUtils.setField(category, "requiredEntitlementType", EntitlementType.PUPPY);
		}
		entityManager.flush();
		entityManager.clear();
	}
}
