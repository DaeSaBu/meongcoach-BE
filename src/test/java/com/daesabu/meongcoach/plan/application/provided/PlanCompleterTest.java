package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
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
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanCompleterTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private PlanCompleter completer;

	@Autowired
	private PlanRepository planRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 모든_커리큘럼을_완료하면_플랜_상태와_완료_시각을_저장한다() {
		Curriculum first = saveCurriculum();
		Curriculum second = saveCurriculum();
		completeLesson(USER_ID, saveLesson(first));
		completeLesson(USER_ID, saveLesson(second));
		Plan plan = savePlan(USER_ID, first, second);
		flushAndClear();
		LocalDateTime before = LocalDateTime.now().minusSeconds(1);

		completer.complete(USER_ID);
		flushAndClear();

		Plan completed = planRepository.findById(plan.getId()).orElseThrow();
		assertThat(completed.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(completed.getCompletedAt()).isBetween(before, LocalDateTime.now().plusSeconds(1));
	}

	@Test
	void 미완료_커리큘럼이_있으면_활성_상태를_유지한다() {
		Curriculum first = saveCurriculum();
		Curriculum second = saveCurriculum();
		completeLesson(USER_ID, saveLesson(first));
		saveLesson(second);
		Plan plan = savePlan(USER_ID, first, second);
		flushAndClear();

		completer.complete(USER_ID);
		flushAndClear();

		Plan active = planRepository.findById(plan.getId()).orElseThrow();
		assertThat(active.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(active.getCompletedAt()).isNull();
	}

	@Test
	void 다른_회원의_학습_완료로_플랜을_완료하지_않는다() {
		Curriculum curriculum = saveCurriculum();
		completeLesson(43L, saveLesson(curriculum));
		Plan plan = savePlan(USER_ID, curriculum);
		flushAndClear();

		completer.complete(USER_ID);
		flushAndClear();

		assertThat(planRepository.findById(plan.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 활성_플랜이_없으면_다른_회원의_플랜을_변경하지_않는다() {
		Curriculum curriculum = saveCurriculum();
		completeLesson(43L, saveLesson(curriculum));
		Plan other = savePlan(43L, curriculum);
		flushAndClear();

		completer.complete(USER_ID);
		flushAndClear();

		assertThat(planRepository.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 반복_호출해도_이미_완료된_플랜의_완료_시각은_유지한다() {
		Curriculum curriculum = saveCurriculum();
		completeLesson(USER_ID, saveLesson(curriculum));
		Plan plan = savePlan(USER_ID, curriculum);
		flushAndClear();
		completer.complete(USER_ID);
		flushAndClear();
		LocalDateTime completedAt = planRepository.findById(plan.getId()).orElseThrow().getCompletedAt();

		completer.complete(USER_ID);
		flushAndClear();

		assertThat(planRepository.findById(plan.getId()).orElseThrow().getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 교체된_플랜은_완료하지_않는다() {
		Curriculum curriculum = saveCurriculum();
		completeLesson(USER_ID, saveLesson(curriculum));
		Plan plan = savePlan(USER_ID, curriculum);
		plan.replace();
		flushAndClear();

		completer.complete(USER_ID);
		flushAndClear();

		Plan replaced = planRepository.findById(plan.getId()).orElseThrow();
		assertThat(replaced.getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertThat(replaced.getCompletedAt()).isNull();
	}

	@Test
	void 잘못된_커리큘럼_데이터는_완료_처리하지_않고_예외를_전달한다() {
		Curriculum curriculum = saveCurriculum();
		Plan plan = savePlan(USER_ID, curriculum);
		flushAndClear();

		assertThatThrownBy(() -> completer.complete(USER_ID))
				.isInstanceOf(IllegalStateException.class);
		assertThat(planRepository.findById(plan.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	private Plan savePlan(Long userId, Curriculum... curriculums) {
		List<Long> curriculumIds = List.of(curriculums).stream().map(Curriculum::getId).toList();
		PlanCreateCommand command = new PlanCreateCommand(userId, 1L, "초코의 2주 훈련 플랜", "목표", 10, 3);
		Plan plan = Plan.create(command, curriculumIds);
		return planRepository.save(plan);
	}

	private Curriculum saveCurriculum() {
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null);
		entityManager.persist(category);
		Topic topic = TopicFixture.create(category, "토픽", 1, null, null, null);
		entityManager.persist(topic);
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼", 1, null, null);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private Lesson saveLesson(Curriculum curriculum) {
		Lesson lesson = LessonFixture.create(curriculum, "레슨", 1, 5);
		entityManager.persist(lesson);
		return lesson;
	}

	private void completeLesson(Long userId, Lesson lesson) {
		LessonProgress progress = LessonProgress.start(userId, lesson.getId());
		progress.complete();
		entityManager.persist(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
