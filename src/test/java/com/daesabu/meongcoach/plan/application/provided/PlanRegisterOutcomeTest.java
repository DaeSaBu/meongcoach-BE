package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeRecordRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeResult;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.InvalidPlanOutcomeException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotCompletedException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.plan.domain.exception.PlanOutcomeAlreadyRecordedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanRegisterOutcomeTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private PlanRegister planRegister;

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 완료된_플랜에_회고를_기록하고_커리큘럼_수와_걸린_날과_토픽을_반환한다() {
		TrainingCategory category = saveCategory();
		Topic walk = saveTopic(category, "산책", 1);
		Topic calm = saveTopic(category, "흥분 조절", 2);
		Curriculum calmFirst = saveCurriculum(calm, 1);
		Curriculum walkFirst = saveCurriculum(walk, 1);
		Curriculum walkSecond = saveCurriculum(walk, 2);
		Plan plan = saveCompletedPlan(Instant.now().plus(Duration.ofDays(2)), calmFirst, walkFirst, walkSecond);
		flushAndClear();

		PlanOutcomeResult result = planRegister.recordOutcome(USER_ID, plan.getId(), request("PARTIAL"));
		flushAndClear();

		assertThat(findPlan(plan).getOutcome()).isEqualTo(PlanOutcome.PARTIAL);
		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(result.plan().getId()).isEqualTo(plan.getId());
		assertThat(result.plan().getCurriculumIds()).hasSize(3);
		assertThat(result.elapsedDays()).isEqualTo(3);
		assertThat(result.topics()).extracting(TopicResult::id).containsExactly(calm.getId(), walk.getId());
		assertThat(result.topics()).extracting(TopicResult::title).containsExactly("흥분 조절", "산책");
	}

	@Test
	void 다른_사용자의_플랜이면_회고를_기록하지_않는다() {
		Plan plan = saveCompletedPlan(Instant.now(), saveCurriculum(saveTopic(saveCategory(), "토픽", 1), 1));
		flushAndClear();

		assertThatThrownBy(() -> planRegister.recordOutcome(OTHER_USER_ID, plan.getId(), request("ACHIEVED")))
				.isInstanceOf(PlanNotFoundException.class);
		assertThat(findPlan(plan).getOutcome()).isNull();
	}

	@Test
	void 완료되지_않은_플랜이면_회고를_기록하지_않는다() {
		Plan plan = planRepository.save(createPlan(saveCurriculum(saveTopic(saveCategory(), "토픽", 1), 1)));
		flushAndClear();

		assertThatThrownBy(() -> planRegister.recordOutcome(USER_ID, plan.getId(), request("ACHIEVED")))
				.isInstanceOf(PlanNotCompletedException.class);
		assertThat(findPlan(plan).getOutcome()).isNull();
	}

	@Test
	void 이미_회고를_기록한_플랜은_다시_기록하지_않는다() {
		Plan plan = saveCompletedPlan(Instant.now(), saveCurriculum(saveTopic(saveCategory(), "토픽", 1), 1));
		flushAndClear();
		planRegister.recordOutcome(USER_ID, plan.getId(), request("NOT_YET"));
		flushAndClear();

		assertThatThrownBy(() -> planRegister.recordOutcome(USER_ID, plan.getId(), request("ACHIEVED")))
				.isInstanceOf(PlanOutcomeAlreadyRecordedException.class);
		flushAndClear();
		assertThat(findPlan(plan).getOutcome()).isEqualTo(PlanOutcome.NOT_YET);
	}

	@Test
	void 정해진_회고_결과가_아니면_기록하지_않는다() {
		Plan plan = saveCompletedPlan(Instant.now(), saveCurriculum(saveTopic(saveCategory(), "토픽", 1), 1));
		flushAndClear();

		assertThatThrownBy(() -> planRegister.recordOutcome(USER_ID, plan.getId(), request("achieved")))
				.isInstanceOf(InvalidPlanOutcomeException.class);
		assertThat(findPlan(plan).getOutcome()).isNull();
	}

	@Test
	void 회고_결과가_비어_있으면_검증에서_거절한다() {
		Plan plan = saveCompletedPlan(Instant.now(), saveCurriculum(saveTopic(saveCategory(), "토픽", 1), 1));
		flushAndClear();

		assertThatThrownBy(() -> planRegister.recordOutcome(USER_ID, plan.getId(), request(" ")))
				.isInstanceOf(ConstraintViolationException.class);
		assertThat(findPlan(plan).getOutcome()).isNull();
	}

	private PlanOutcomeRecordRequest request(String outcome) {
		return new PlanOutcomeRecordRequest(outcome);
	}

	private Plan saveCompletedPlan(Instant completedAt, Curriculum... curriculums) {
		Plan plan = createPlan(curriculums);
		plan.complete(completedAt);
		return planRepository.save(plan);
	}

	private Plan createPlan(Curriculum... curriculums) {
		List<Long> curriculumIds = List.of(curriculums).stream().map(Curriculum::getId).toList();
		PlanCreateCommand command = new PlanCreateCommand(USER_ID, 1L, "초코의 2주 훈련 플랜", "목표", 10, 3);
		return Plan.create(command, curriculumIds);
	}

	private TrainingCategory saveCategory() {
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null);
		entityManager.persist(category);
		return category;
	}

	private Topic saveTopic(TrainingCategory category, String title, int order) {
		Topic topic = TopicFixture.create(category, title, order, null, null, null);
		entityManager.persist(topic);
		return topic;
	}

	private Curriculum saveCurriculum(Topic topic, int order) {
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼 " + order, order, null, null, false);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private Plan findPlan(Plan plan) {
		return planRepository.findById(plan.getId()).orElseThrow();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
