package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanRegisterRetrainingTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private PlanRegister register;
	@Autowired
	private CurrentPlanFinder finder;
	@Autowired
	private AiPlanGenerator generator;
	@Autowired
	private EntityManager entityManager;

	private Dog dog;
	private Topic topic;
	private Curriculum curriculum;
	private LessonProgress progress;

	@BeforeEach
	void 완료한_토픽을_준비한다() {
		DogRegisterCommand command = new DogRegisterCommand(USER_ID, "초코", new Breed("푸들"),
				DogSex.MALE, null, null, null, null);
		dog = Dog.register(command);
		entityManager.persist(dog);
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null);
		entityManager.persist(category);
		topic = TopicFixture.create(category, "기다리기", 1, null, null, null);
		entityManager.persist(topic);
		curriculum = CurriculumFixture.create(topic, "짧게 기다리기", 1, null, null);
		entityManager.persist(curriculum);
		Lesson lesson = LessonFixture.create(curriculum, "레슨", 1, 5);
		entityManager.persist(lesson);
		progress = LessonProgress.start(USER_ID, lesson.getId());
		progress.complete();
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void 이미_배운_토픽으로_수동_플랜을_생성하고_진도를_처음부터_반환한다() {
		CustomPlanCreateRequest request = new CustomPlanCreateRequest(List.of(topic.getId()), 10, 3);

		Plan created = register.createCustom(USER_ID, dog.getId(), request);

		assertRetrainingPlan(created);
	}

	@Test
	void 다른_미완료_토픽이_있어도_AI가_배운_토픽만_선택하면_복습_플랜을_생성한다() {
		TrainingCategory category = entityManager.find(TrainingCategory.class, topic.getTrainingCategory().getId());
		Topic incompleteTopic = TopicFixture.create(category, "다가오기", 2, null, null, null);
		entityManager.persist(incompleteTopic);
		Curriculum incomplete = CurriculumFixture.create(incompleteTopic, "다가오기 연습", 1, null, null);
		entityManager.persist(incomplete);
		entityManager.persist(LessonFixture.create(incomplete, "레슨", 1, 5));
		entityManager.flush();
		entityManager.clear();
		given(generator.generate(any())).willReturn(new AiPlanGenerateResult(List.of(topic.getId()), "기다리기를 복습해요"));

		Plan created = register.createAiPlan(USER_ID, dog.getId(), new AiPlanCreateRequest(10, 3, "기다리기 복습"));

		assertRetrainingPlan(created);
	}

	private void assertRetrainingPlan(Plan created) {
		entityManager.flush();
		entityManager.clear();
		var current = finder.find(USER_ID, dog.getId()).orElseThrow();

		assertThat(current.plan().getId()).isEqualTo(created.getId());
		assertThat(current.plan().getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(current.plan().getCurriculumIds()).containsExactly(curriculum.getId());
		assertThat(current.plan().getCompletedLessonIds()).isEmpty();
		assertThat(current.curriculums()).hasSize(1);
		assertThat(current.curriculums().getFirst().status()).isEqualTo(ProgressStatus.NOT_STARTED);
		assertThat(current.curriculums().getFirst().completedLessons()).isZero();
		assertThat(current.curriculums().getFirst().lessons()).allSatisfy(lesson -> assertThat(lesson.completed()).isFalse());
		assertThat(entityManager.find(LessonProgress.class, progress.getId()).getCompletedCount()).isEqualTo(1);
	}
}
