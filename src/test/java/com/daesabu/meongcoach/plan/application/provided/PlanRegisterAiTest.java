package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateRequest;
import com.daesabu.meongcoach.plan.application.required.dto.AiPlanGenerateResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidAiPlanResponseException;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class PlanRegisterAiTest {

	private static final long USER_ID = 627001L;
	private AiPlanCreateRequest request;
	@Autowired
	private PlanRegister register;
	@Autowired
	private AiPlanGenerator generator;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private JdbcTemplate jdbc;
	@MockitoSpyBean
	private PlanRepository planRepository;

	private Long categoryId;
	private Long topicId;
	private Long dogId;
	private final List<Long> curriculumIds = new ArrayList<>();
	private final List<Long> lessonIds = new ArrayList<>();

	@BeforeEach
	void 준비한다() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			DogRegisterCommand dogCommand = new DogRegisterCommand(USER_ID, "초코", new Breed("푸들"),
					DogSex.MALE, LocalDate.of(2025, 1, 31), null, null, null);
			Dog dog = Dog.register(dogCommand);
			entityManager.persist(dog);
			dogId = dog.getId();
			request = new AiPlanCreateRequest("  차분하게 기다렸으면 해요  ");
			TrainingCategory category = TrainingCategoryFixture.create("AI 플랜 테스트", 1, "", "");
			entityManager.persist(category);
			categoryId = category.getId();
			Topic topic = TopicFixture.create(category, "기다리기", 1, "", "", "");
			entityManager.persist(topic);
			topicId = topic.getId();
			for (int order = 1; order <= 3; order++) {
				Curriculum curriculum = CurriculumFixture.create(topic, "기다리기 " + order, order, "", "연습");
				entityManager.persist(curriculum);
				curriculumIds.add(curriculum.getId());
				Lesson lesson = LessonFixture.create(curriculum, "레슨", 1, 5);
				entityManager.persist(lesson);
				lessonIds.add(lesson.getId());
			}
			completeLesson(lessonIds.getFirst());
			completeLesson(lessonIds.getLast());
		});
		given(generator.generate(any())).willReturn(new AiPlanGenerateResult(List.of(topicId), "차분히 기다리는 습관을 길러요"));
	}

	@AfterEach
	void 정리한다() {
		jdbc.update("DELETE FROM plan_curriculums WHERE plan_id IN (SELECT id FROM plans WHERE user_id = ?)", USER_ID);
		jdbc.update("DELETE FROM plans WHERE user_id = ?", USER_ID);
		jdbc.update("DELETE FROM lesson_progress WHERE user_id = ?", USER_ID);
		jdbc.update("DELETE FROM lessons WHERE curriculum_id IN (SELECT c.id FROM curriculums c JOIN topics t "
				+ "ON c.topic_id = t.id WHERE t.training_category_id = ?)", categoryId);
		jdbc.update("DELETE FROM curriculums WHERE topic_id IN (SELECT id FROM topics WHERE training_category_id = ?)",
				categoryId);
		jdbc.update("DELETE FROM topics WHERE training_category_id = ?", categoryId);
		jdbc.update("DELETE FROM training_categories WHERE id = ?", categoryId);
		jdbc.update("DELETE FROM dogs WHERE user_id = ?", USER_ID);
	}

	@Test
	void AI는_트랜잭션_밖에서_호출하고_선택한_토픽의_커리큘럼을_모두_저장한다() {
		Plan existing = saveExistingPlan();
		LocalDate before = LocalDate.now(ZoneId.of("Asia/Seoul"));
		given(generator.generate(any())).willAnswer(invocation -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			assertActivePlan(existing.getId());
			AiPlanGenerateRequest input = invocation.getArgument(0);
			assertThat(input.dog().id()).isEqualTo(dogId);
			assertThat(input.dog().birthDate()).isEqualTo(LocalDate.of(2025, 1, 31));
			assertThat(input.dog().weightKg()).isNull();
			assertThat(input.expectation()).isEqualTo(request.expectation());
			assertThat(input.generationDate()).isBetween(before, LocalDate.now(ZoneId.of("Asia/Seoul")));
			return new AiPlanGenerateResult(List.of(topicId), "차분히 기다리는 습관을 길러요");
		});

		Plan created = register.createAiPlan(USER_ID, dogId, request);

		assertThat(created.getTitle()).isEqualTo("초코의 훈련 플랜");
		assertThat(created.getCurriculumIds()).containsExactlyElementsOf(curriculumIds);
		assertThat(planRepository.findById(created.getId()).orElseThrow().getExpectation()).isEqualTo(request.expectation());
		assertThat(planRepository.findById(existing.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertActivePlan(created.getId());
		assertThat(jdbc.queryForObject("SELECT sum(completed_count) FROM lesson_progress WHERE user_id = ?", Long.class, USER_ID))
				.isEqualTo(2L);
	}

	@Test
	void AI가_반환한_토픽_순서대로_커리큘럼을_저장한다() {
		Curriculum earlierCurriculum = new TransactionTemplate(transactionManager).execute(status -> {
			TrainingCategory category = entityManager.find(TrainingCategory.class, categoryId);
			Topic earlierTopic = TopicFixture.create(category, "앉아", 0, "", "", "");
			entityManager.persist(earlierTopic);
			Curriculum curriculum = CurriculumFixture.create(earlierTopic, "앉아 1", 1, "", "연습");
			entityManager.persist(curriculum);
			entityManager.persist(LessonFixture.create(curriculum, "레슨", 1, 5));
			return curriculum;
		});
		Long earlierTopicId = earlierCurriculum.getTopic().getId();
		given(generator.generate(any())).willReturn(
				new AiPlanGenerateResult(List.of(topicId, earlierTopicId), "차분히 기다리는 습관을 길러요"));

		Plan created = register.createAiPlan(USER_ID, dogId, request);

		List<Long> expected = new ArrayList<>(curriculumIds);
		expected.add(earlierCurriculum.getId());
		List<Long> saved = jdbc.queryForList(
				"SELECT curriculum_id FROM plan_curriculums WHERE plan_id = ? ORDER BY sort_order", Long.class,
				created.getId());
		assertThat(saved).containsExactlyElementsOf(expected);
	}

	@Test
	void 선택_토픽_밖의_후보도_AI에_전달하고_추가한_토픽을_함께_저장한다() {
		Curriculum earlierCurriculum = new TransactionTemplate(transactionManager).execute(status -> {
			TrainingCategory category = entityManager.find(TrainingCategory.class, categoryId);
			Topic earlierTopic = TopicFixture.create(category, "앉아", 0, "", "", "");
			entityManager.persist(earlierTopic);
			Curriculum curriculum = CurriculumFixture.create(earlierTopic, "앉아 1", 1, "", "연습");
			entityManager.persist(curriculum);
			entityManager.persist(LessonFixture.create(curriculum, "레슨", 1, 5));
			return curriculum;
		});
		Long earlierTopicId = earlierCurriculum.getTopic().getId();
		AiPlanCreateRequest selected = new AiPlanCreateRequest("앉아서 기다렸으면 해요", List.of(topicId));
		given(generator.generate(any())).willAnswer(invocation -> {
			AiPlanGenerateRequest input = invocation.getArgument(0);
			assertThat(input.selectedTopicIds()).containsExactly(topicId);
			assertThat(input.curriculums()).extracting(item -> item.topicId()).contains(topicId, earlierTopicId);
			return new AiPlanGenerateResult(List.of(topicId, earlierTopicId), "차분히 기다리는 습관을 길러요");
		});

		Plan created = register.createAiPlan(USER_ID, dogId, selected);

		List<Long> expected = new ArrayList<>(curriculumIds);
		expected.add(earlierCurriculum.getId());
		List<Long> saved = jdbc.queryForList(
				"SELECT curriculum_id FROM plan_curriculums WHERE plan_id = ? ORDER BY sort_order", Long.class,
				created.getId());
		assertThat(saved).containsExactlyElementsOf(expected);
	}

	@Test
	void 보호자가_선택한_토픽을_AI에_전달하고_플랜에_보존한다() {
		AiPlanCreateRequest selected = new AiPlanCreateRequest("차분하게 기다려요", List.of(topicId));
		given(generator.generate(any())).willAnswer(invocation -> {
			AiPlanGenerateRequest input = invocation.getArgument(0);
			assertThat(input.selectedTopicIds()).containsExactly(topicId);
			assertThat(input.curriculums()).extracting(item -> item.topicId()).contains(topicId);
			return new AiPlanGenerateResult(List.of(topicId), "차분히 기다려요");
		});

		Plan created = register.createAiPlan(USER_ID, dogId, selected);

		assertThat(created.getCurriculumIds()).containsExactlyElementsOf(curriculumIds);
	}

	@Test
	void 없는_토픽을_선택하면_AI를_호출하지_않는다() {
		AiPlanCreateRequest selected = new AiPlanCreateRequest("차분하게 기다려요", List.of(Long.MAX_VALUE));

		assertThatThrownBy(() -> register.createAiPlan(USER_ID, dogId, selected)).isInstanceOf(RuntimeException.class);

		verifyNoInteractions(generator);
	}

	@Test
	void AI_실패이면_기존_플랜을_유지한다() {
		Plan existing = saveExistingPlan();
		InvalidAiPlanResponseException failure = new InvalidAiPlanResponseException();
		given(generator.generate(any())).willThrow(failure);

		assertThatThrownBy(() -> register.createAiPlan(USER_ID, dogId, request)).isSameAs(failure);

		assertActivePlan(existing.getId());
		assertThat(planRepository.count()).isEqualTo(1);
	}

	@Test
	void 새_플랜_저장_실패이면_기존_플랜_교체도_롤백한다() {
		Plan existing = saveExistingPlan();
		DataIntegrityViolationException failure = new DataIntegrityViolationException("저장 실패");
		doAnswer(invocation -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			throw failure;
		}).when(planRepository).saveAndFlush(any(Plan.class));

		assertThatThrownBy(() -> register.createAiPlan(USER_ID, dogId, request))
				.isInstanceOf(PlanCreationConflictException.class).hasCause(failure);

		assertActivePlan(existing.getId());
	}

	@Test
	void 모든_커리큘럼이_완료됐어도_AI_플랜을_생성한다() {
		Plan existing = saveExistingPlan();
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> completeLesson(lessonIds.get(1)));

		Plan created = register.createAiPlan(USER_ID, dogId, request);

		assertThat(created.getCurriculumIds()).containsExactlyElementsOf(curriculumIds);
		assertThat(created.getCompletedLessonIds()).isEmpty();
		assertThat(planRepository.findById(existing.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertActivePlan(created.getId());
		assertThat(jdbc.queryForObject("SELECT sum(completed_count) FROM lesson_progress WHERE user_id = ?", Long.class, USER_ID))
				.isEqualTo(3L);
	}

	@Test
	void AI_호출_중_남은_훈련이_완료되어도_복습_플랜을_생성한다() {
		Plan existing = saveExistingPlan();
		given(generator.generate(any())).willAnswer(invocation -> {
			new TransactionTemplate(transactionManager).executeWithoutResult(status -> completeLesson(lessonIds.get(1)));
			return new AiPlanGenerateResult(List.of(topicId), "기다리기");
		});

		Plan created = register.createAiPlan(USER_ID, dogId, request);

		assertThat(created.getCurriculumIds()).containsExactlyElementsOf(curriculumIds);
		assertThat(created.getCompletedLessonIds()).isEmpty();
		assertThat(planRepository.findById(existing.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertActivePlan(created.getId());
	}

	@Test
	void 커리큘럼_후보가_없으면_AI를_호출하지_않고_기존_플랜을_유지한다() {
		Plan existing = saveExistingPlan();
		jdbc.update("DELETE FROM lessons WHERE curriculum_id IN (SELECT id FROM curriculums WHERE topic_id = ?)", topicId);
		jdbc.update("DELETE FROM curriculums WHERE topic_id = ?", topicId);

		assertThatThrownBy(() -> register.createAiPlan(USER_ID, dogId, request)).isInstanceOf(EmptyPlanCurriculumsException.class);

		verifyNoInteractions(generator);
		assertActivePlan(existing.getId());
	}

	@Test
	void 소유하지_않은_강아지면_AI를_호출하지_않는다() {
		assertThatThrownBy(() -> register.createAiPlan(USER_ID + 1, dogId, request)).isInstanceOf(DogNotFoundException.class);
		verifyNoInteractions(generator);
	}

	@ParameterizedTest
	@MethodSource("invalidRequests")
	void 잘못된_요청은_AI_호출_전에_거절한다(AiPlanCreateRequest request) {
		assertThatThrownBy(() -> register.createAiPlan(USER_ID, dogId, request)).isInstanceOf(RuntimeException.class);
		verifyNoInteractions(generator);
	}

	private static Stream<AiPlanCreateRequest> invalidRequests() {
		return Stream.of(new AiPlanCreateRequest(null), new AiPlanCreateRequest("  "),
				new AiPlanCreateRequest("가".repeat(501)));
	}

	private Plan saveExistingPlan() {
		PlanCreateCommand command = new PlanCreateCommand(USER_ID, dogId, "기존 플랜", "기존 목표");
		Plan plan = Plan.create(command, List.of(curriculumIds.getFirst()));
		return planRepository.saveAndFlush(plan);
	}

	private void assertActivePlan(Long expectedId) {
		assertThat(planRepository.findByUserIdAndDogIdAndStatus(USER_ID, dogId, PlanStatus.ACTIVE).orElseThrow().getId())
				.isEqualTo(expectedId);
	}

	private void completeLesson(Long lessonId) {
		LessonProgress progress = LessonProgress.start(USER_ID, lessonId);
		progress.complete();
		entityManager.persist(progress);
	}
}
