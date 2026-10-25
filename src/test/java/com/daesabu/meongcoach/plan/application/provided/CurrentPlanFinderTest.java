package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanLessonProgressResult;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurrentPlanFinderTest {

	@Autowired
	private CurrentPlanFinder finder;
	@Autowired
	private PlanRepository planRepository;
	@Autowired
	private EntityManager entityManager;

	@Test
	void 완료한_플랜보다_활성_플랜을_우선_반환한다() {
		Long curriculumId = saveCurriculum();
		Plan completed = savePlan(42L, curriculumId);
		completed.complete(Instant.now());
		Plan active = savePlan(42L, curriculumId);
		flushAndClear();

		var result = finder.find(42L).orElseThrow();

		assertThat(result.plan().getId()).isEqualTo(active.getId());
		assertThat(result.curriculums()).hasSize(1);
		assertThat(result.curriculums().getFirst().description()).isEqualTo("훈련 설명");
	}

	@Test
	void 생성_순서가_아닌_완료_시각이_가장_최근인_플랜을_반환한다() {
		Long curriculumId = saveCurriculum();
		Plan latest = savePlan(42L, curriculumId);
		latest.complete(Instant.parse("2026-10-23T12:00:00Z"));
		Plan older = savePlan(42L, curriculumId);
		older.complete(Instant.parse("2026-10-22T12:00:00Z"));
		savePlan(42L, curriculumId).replace();
		savePlan(43L, curriculumId);
		flushAndClear();

		assertThat(finder.find(42L).orElseThrow().plan().getId()).isEqualTo(latest.getId());
	}

	@Test
	void 완료_시각이_같으면_ID가_큰_플랜을_반환한다() {
		Long curriculumId = saveCurriculum();
		Instant time = Instant.parse("2026-10-23T12:00:00Z");
		savePlan(42L, curriculumId).complete(time);
		Plan latest = savePlan(42L, curriculumId);
		latest.complete(time);
		flushAndClear();

		assertThat(finder.find(42L).orElseThrow().plan().getId()).isEqualTo(latest.getId());
	}

	@Test
	void 다른_사용자_플랜과_교체된_플랜은_현재_플랜으로_반환하지_않는다() {
		Long curriculumId = saveCurriculum();
		savePlan(42L, curriculumId).replace();
		savePlan(43L, curriculumId);
		flushAndClear();

		assertThat(finder.find(42L)).isEmpty();
		assertThat(finder.find(99L)).isEmpty();
	}

	@Test
	void 일반_학습과_이전_플랜의_완료는_현재_플랜_진도에_반영하지_않는다() {
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long lessonId = curriculum.getLessonIds().getFirst();
		LessonProgress progress = LessonProgress.start(42L, lessonId);
		progress.complete();
		entityManager.persist(progress);
		Plan previous = savePlan(42L, curriculumId);
		previous.completeLesson(lessonId);
		previous.replace();
		savePlan(42L, curriculumId);
		flushAndClear();

		var result = finder.find(42L).orElseThrow().curriculums().getFirst();

		assertThat(result.completedLessons()).isZero();
		assertThat(result.status()).isEqualTo(ProgressStatus.NOT_STARTED);
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(false);
	}

	@Test
	void 현재_플랜의_완료_기록으로_부분_완료와_전체_완료를_계산한다() {
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long firstLessonId = curriculum.getLessonIds().getFirst();
		Lesson second = LessonFixture.create(curriculum, "두 번째", 2, 5);
		entityManager.persist(second);
		Plan plan = savePlan(42L, curriculumId);
		plan.completeLesson(firstLessonId);
		flushAndClear();

		var partial = finder.find(42L).orElseThrow().curriculums().getFirst();

		assertThat(partial.totalLessons()).isEqualTo(2);
		assertThat(partial.completedLessons()).isEqualTo(1);
		assertThat(partial.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
		assertThat(partial.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(true, false);
		entityManager.find(Plan.class, plan.getId()).completeLesson(second.getId());
		flushAndClear();

		var completed = finder.find(42L).orElseThrow().curriculums().getFirst();

		assertThat(completed.completedLessons()).isEqualTo(2);
		assertThat(completed.status()).isEqualTo(ProgressStatus.COMPLETED);
	}

	@Test
	void 현재_목록에서_빠진_레슨은_완료_수에서_제외하고_추가된_레슨은_미완료로_표시한다() {
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long removedLessonId = curriculum.getLessonIds().getFirst();
		Plan plan = savePlan(42L, curriculumId);
		plan.completeLesson(removedLessonId);
		entityManager.remove(entityManager.find(Lesson.class, removedLessonId));
		Lesson added = LessonFixture.create(curriculum, "새 레슨", 1, 5);
		entityManager.persist(added);
		flushAndClear();

		var result = finder.find(42L).orElseThrow().curriculums().getFirst();

		assertThat(result.totalLessons()).isEqualTo(1);
		assertThat(result.completedLessons()).isZero();
		assertThat(result.status()).isEqualTo(ProgressStatus.NOT_STARTED);
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::lessonId).containsExactly(added.getId());
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(false);
	}

	private Plan savePlan(Long userId, Long curriculumId) {
		PlanCreateCommand command = new PlanCreateCommand(userId, 1L, "초코의 2주 훈련 플랜", "목표", 10, 3);
		Plan plan = Plan.create(command, List.of(curriculumId));
		planRepository.flush();
		return planRepository.save(plan);
	}

	private Long saveCurriculum() {
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null);
		entityManager.persist(category);
		Topic topic = TopicFixture.create(category, "토픽", 1, null, null, null);
		entityManager.persist(topic);
		Curriculum curriculum = CurriculumFixture.create(topic, "훈련", 1, null, "훈련 설명");
		entityManager.persist(curriculum);
		entityManager.persist(LessonFixture.create(curriculum, "레슨", 1, 5));
		return curriculum.getId();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
