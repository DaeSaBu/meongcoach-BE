package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanLessonOrderException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@ApplicationTest
class PlanLessonCompleterTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private PlanLessonCompleter completer;

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private LessonProgressRepository progressRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void 현재_레슨을_완료하면_일반_학습과_플랜_기록을_함께_저장한다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson first = saveLesson(curriculum, 1);
		saveLesson(curriculum, 2);
		Plan plan = savePlan(curriculum);
		flushAndClear();

		completer.complete(USER_ID, plan.getId(), first.getId());
		flushAndClear();

		Plan found = findPlan(plan);
		assertThat(found.getCompletedLessonIds()).containsExactly(first.getId());
		assertThat(found.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(found.getCompletedAt()).isNull();
		assertThat(completedCount(first)).isOne();
	}

	@Test
	void 플랜의_커리큘럼_순서대로_완료하고_마지막에_플랜을_완료한다() {
		Curriculum first = saveCurriculum(false);
		Lesson firstLesson = saveLesson(first, 1);
		Curriculum second = saveCurriculum(false);
		Lesson secondLesson = saveLesson(second, 1);
		Plan plan = savePlan(second, first);
		flushAndClear();

		completer.complete(USER_ID, plan.getId(), secondLesson.getId());
		flushAndClear();
		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.ACTIVE);
		Instant before = Instant.now().minusSeconds(1);
		completer.complete(USER_ID, plan.getId(), firstLesson.getId());
		flushAndClear();

		Plan found = findPlan(plan);
		assertThat(found.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(found.getCompletedAt()).isBetween(before, Instant.now().plusSeconds(1));
		assertThat(found.getCompletedLessonIds()).containsExactlyInAnyOrder(firstLesson.getId(), secondLesson.getId());
		assertThat(completedCount(firstLesson)).isOne();
		assertThat(completedCount(secondLesson)).isOne();
	}

	@Test
	void 활성_플랜의_이미_완료한_레슨은_횟수를_다시_늘리지_않는다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson first = saveLesson(curriculum, 1);
		saveLesson(curriculum, 2);
		Plan plan = savePlan(curriculum);
		flushAndClear();
		completer.complete(USER_ID, plan.getId(), first.getId());
		flushAndClear();

		completer.complete(USER_ID, plan.getId(), first.getId());
		flushAndClear();

		assertThat(completedCount(first)).isOne();
		assertThat(findPlan(plan).getCompletedLessonIds()).containsExactly(first.getId());
		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 마지막_레슨을_재요청해도_횟수와_완료_시각을_유지한다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson lesson = saveLesson(curriculum, 1);
		Plan plan = savePlan(curriculum);
		flushAndClear();
		completer.complete(USER_ID, plan.getId(), lesson.getId());
		flushAndClear();
		Instant completedAt = findPlan(plan).getCompletedAt();

		completer.complete(USER_ID, plan.getId(), lesson.getId());
		flushAndClear();

		assertThat(completedCount(lesson)).isOne();
		assertThat(findPlan(plan).getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 없는_플랜이나_다른_사용자의_플랜은_조회할_수_없다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson lesson = saveLesson(curriculum, 1);
		Plan plan = savePlan(curriculum);
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID, -1L, lesson.getId()))
				.isInstanceOf(PlanNotFoundException.class);
		assertThatThrownBy(() -> completer.complete(43L, plan.getId(), lesson.getId()))
				.isInstanceOf(PlanNotFoundException.class);
		assertThat(findPlan(plan).getCompletedLessonIds()).isEmpty();
		assertThat(progressRepository.findByUserIdAndLessonId(USER_ID, lesson.getId())).isEmpty();
	}

	@Test
	void 교체된_플랜은_이미_완료한_레슨의_재요청도_거절한다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson lesson = saveLesson(curriculum, 1);
		Plan plan = savePlan(curriculum);
		plan.completeLesson(lesson.getId());
		plan.replace();
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID, plan.getId(), lesson.getId()))
				.isInstanceOf(PlanNotActiveException.class);
		assertThat(progressRepository.findByUserIdAndLessonId(USER_ID, lesson.getId())).isEmpty();
	}

	@Test
	void 완료된_플랜에는_새_레슨을_완료할_수_없다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson lesson = saveLesson(curriculum, 1);
		Plan plan = savePlan(curriculum);
		plan.complete(Instant.now());
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID, plan.getId(), lesson.getId()))
				.isInstanceOf(PlanNotActiveException.class);
		assertThat(findPlan(plan).getCompletedLessonIds()).isEmpty();
	}

	@Test
	void 현재_차례가_아니거나_목록에_없는_레슨은_거절한다() {
		Curriculum curriculum = saveCurriculum(false);
		saveLesson(curriculum, 1);
		Lesson later = saveLesson(curriculum, 2);
		Plan plan = savePlan(curriculum);
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID, plan.getId(), later.getId()))
				.isInstanceOf(PlanLessonOrderException.class);
		assertThatThrownBy(() -> completer.complete(USER_ID, plan.getId(), -1L))
				.isInstanceOf(PlanLessonOrderException.class);
		assertThat(findPlan(plan).getCompletedLessonIds()).isEmpty();
		assertThat(progressRepository.findByUserIdAndLessonId(USER_ID, later.getId())).isEmpty();
	}

	@Test
	void 일반_학습에서_완료한_레슨도_플랜에서는_순서대로_다시_완료한다() {
		Curriculum curriculum = saveCurriculum(false);
		Lesson first = saveLesson(curriculum, 1);
		Lesson later = saveLesson(curriculum, 2);
		LessonProgress progress = LessonProgress.start(USER_ID, later.getId());
		progress.complete();
		progressRepository.save(progress);
		Plan plan = savePlan(curriculum);
		flushAndClear();

		completer.complete(USER_ID, plan.getId(), first.getId());
		flushAndClear();
		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.ACTIVE);
		completer.complete(USER_ID, plan.getId(), later.getId());
		flushAndClear();

		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(completedCount(later)).isEqualTo(2);
	}

	@Test
	void 이용권이_없으면_일반_학습과_플랜_기록을_변경하지_않는다() {
		Curriculum curriculum = saveCurriculum(true);
		Lesson lesson = saveLesson(curriculum, 1);
		Plan plan = savePlan(curriculum);
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID, plan.getId(), lesson.getId()))
				.isInstanceOf(EntitlementRequiredException.class);
		assertThat(findPlan(plan).getCompletedLessonIds()).isEmpty();
		assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(progressRepository.findByUserIdAndLessonId(USER_ID, lesson.getId())).isEmpty();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void 같은_트랜잭션이_실패하면_flush된_일반_학습과_플랜_저장을_모두_롤백한다() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		AtomicReference<Long> planId = new AtomicReference<>();
		AtomicReference<Long> lessonId = new AtomicReference<>();

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			Curriculum curriculum = saveCurriculum(false);
			Lesson lesson = saveLesson(curriculum, 1);
			Plan plan = savePlan(curriculum);
			planId.set(plan.getId());
			lessonId.set(lesson.getId());
			flushAndClear();
			completer.complete(USER_ID, planId.get(), lessonId.get());
			flushAndClear();
			assertThat(findPlan(plan).getStatus()).isEqualTo(PlanStatus.COMPLETED);
			assertThat(completedCount(lesson)).isOne();
			throw new IllegalStateException("트랜잭션 롤백 검증");
		})).isInstanceOf(IllegalStateException.class).hasMessage("트랜잭션 롤백 검증");

		transaction.executeWithoutResult(status -> {
			assertThat(planRepository.findById(planId.get())).isEmpty();
			assertThat(progressRepository.findByUserIdAndLessonId(USER_ID, lessonId.get())).isEmpty();
		});
	}

	private Plan savePlan(Curriculum... curriculums) {
		List<Long> curriculumIds = List.of(curriculums).stream().map(Curriculum::getId).toList();
		PlanCreateCommand command = new PlanCreateCommand(USER_ID, 1L, "초코의 2주 훈련 플랜", "목표", 10, 3);
		Plan plan = Plan.create(command, curriculumIds);
		return planRepository.save(plan);
	}

	private Curriculum saveCurriculum(boolean premium) {
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null, EntitlementType.PUPPY);
		entityManager.persist(category);
		Topic topic = TopicFixture.create(category, "토픽", 1, null, null, null);
		entityManager.persist(topic);
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼", 1, null, null, premium);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private Lesson saveLesson(Curriculum curriculum, int order) {
		Lesson lesson = LessonFixture.create(curriculum, "레슨", order, 5);
		entityManager.persist(lesson);
		return lesson;
	}

	private Plan findPlan(Plan plan) {
		return planRepository.findById(plan.getId()).orElseThrow();
	}

	private int completedCount(Lesson lesson) {
		return progressRepository.findByUserIdAndLessonId(USER_ID, lesson.getId())
				.orElseThrow().getCompletedCount();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
