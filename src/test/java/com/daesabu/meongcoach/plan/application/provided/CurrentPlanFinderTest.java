package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanLessonProgressResult;
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
	void 강아지별로_자신의_활성_플랜만_반환한다() {
		Long firstDog = saveDog(42L);
		Long secondDog = saveDog(42L);
		Long curriculum = saveCurriculum();
		Plan first = savePlan(42L, firstDog, curriculum);
		Plan second = savePlan(42L, secondDog, curriculum);
		flushAndClear();

		assertThat(finder.find(42L, firstDog).orElseThrow().plan().getId()).isEqualTo(first.getId());
		assertThat(finder.find(42L, secondDog).orElseThrow().plan().getId()).isEqualTo(second.getId());
	}

	@Test
	void 선택한_강아지의_플랜이_없으면_다른_강아지의_플랜을_반환하지_않는다() {
		Long firstDog = saveDog(42L);
		Long secondDog = saveDog(42L);
		savePlan(42L, firstDog, saveCurriculum());
		flushAndClear();

		assertThat(finder.find(42L, secondDog)).isEmpty();
	}

	@Test
	void 교체된_플랜만_있는_강아지는_현재_플랜이_없다() {
		Long firstDog = saveDog(42L);
		Long secondDog = saveDog(42L);
		Long curriculum = saveCurriculum();
		Plan replaced = savePlan(42L, firstDog, curriculum);
		replaced.replace();
		savePlan(42L, secondDog, curriculum);
		flushAndClear();

		assertThat(finder.find(42L, firstDog)).isEmpty();
		assertThat(planRepository.findById(replaced.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
	}

	@Test
	void 다른_강아지의_활성_플랜보다_요청한_강아지의_완료_플랜을_반환한다() {
		Long firstDog = saveDog(42L);
		Long secondDog = saveDog(42L);
		Long curriculum = saveCurriculum();
		Plan completed = savePlan(42L, firstDog, curriculum);
		completed.complete(Instant.now());
		savePlan(42L, secondDog, curriculum);
		flushAndClear();

		assertThat(finder.find(42L, firstDog).orElseThrow().plan().getId()).isEqualTo(completed.getId());
	}

	@Test
	void 다른_사용자의_강아지_플랜은_조회할_수_없다() {
		Long dogId = saveDog(43L);

		assertThatThrownBy(() -> finder.find(42L, dogId)).isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 완료한_플랜보다_활성_플랜을_우선_반환한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		Plan completed = savePlan(42L, dogId, curriculumId);
		completed.complete(Instant.now());
		Plan active = savePlan(42L, dogId, curriculumId);
		flushAndClear();

		var result = finder.find(42L, dogId).orElseThrow();

		assertThat(result.plan().getId()).isEqualTo(active.getId());
		assertThat(result.curriculums()).hasSize(1);
		assertThat(result.curriculums().getFirst().description()).isEqualTo("훈련 설명");
	}

	@Test
	void 커리큘럼마다_속한_토픽의_ID와_제목을_반환한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		savePlan(42L, dogId, curriculumId);
		flushAndClear();
		Topic topic = entityManager.find(Curriculum.class, curriculumId).getTopic();

		var result = finder.find(42L, dogId).orElseThrow().curriculums().getFirst();

		assertThat(result.topicId()).isEqualTo(topic.getId());
		assertThat(result.topicTitle()).isEqualTo("토픽");
	}

	@Test
	void 강아지의_활성_플랜에_든_토픽_ID를_반환한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		savePlan(42L, dogId, curriculumId);
		flushAndClear();
		Long topicId = entityManager.find(Curriculum.class, curriculumId).getTopic().getId();

		assertThat(finder.findActiveTopicIds(42L, dogId)).containsExactly(topicId);
	}

	@Test
	void 활성_플랜이_없으면_완료한_플랜이_있어도_빈_토픽_목록을_반환한다() {
		Long dogId = saveDog(42L);
		savePlan(42L, dogId, saveCurriculum()).complete(Instant.now());
		Long otherDog = saveDog(42L);
		savePlan(42L, otherDog, saveCurriculum());
		flushAndClear();

		assertThat(finder.findActiveTopicIds(42L, dogId)).isEmpty();
	}

	@Test
	void 다른_사용자의_강아지로_활성_플랜_토픽을_조회할_수_없다() {
		Long dogId = saveDog(43L);

		assertThatThrownBy(() -> finder.findActiveTopicIds(42L, dogId)).isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 생성_순서가_아닌_완료_시각이_가장_최근인_플랜을_반환한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		Plan latest = savePlan(42L, dogId, curriculumId);
		latest.complete(Instant.parse("2026-10-23T12:00:00Z"));
		Plan older = savePlan(42L, dogId, curriculumId);
		older.complete(Instant.parse("2026-10-22T12:00:00Z"));
		savePlan(42L, dogId, curriculumId).replace();
		savePlan(43L, curriculumId);
		flushAndClear();

		assertThat(finder.find(42L, dogId).orElseThrow().plan().getId()).isEqualTo(latest.getId());
	}

	@Test
	void 완료_후_레슨이_추가되어도_완료_정보만_반환하고_저장된_진도는_유지한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long completedLessonId = curriculum.getLessonIds().getFirst();
		Plan plan = savePlan(42L, dogId, curriculumId);
		plan.completeLesson(completedLessonId);
		Instant completedAt = Instant.parse("2026-10-23T12:00:00Z");
		plan.complete(completedAt);
		entityManager.persist(LessonFixture.create(curriculum, "추가 레슨", 2, 5));
		flushAndClear();

		var result = finder.find(42L, dogId).orElseThrow();

		assertThat(result.curriculums()).isEmpty();
		assertThat(result.plan().getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(result.plan().getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.plan().getCurriculumIds()).containsExactly(curriculumId);
		assertThat(result.plan().getCompletedLessonIds()).containsExactly(completedLessonId);
	}

	@Test
	void 완료된_플랜은_기존_커리큘럼이_없어도_조회할_수_있다() {
		Long dogId = saveDog(42L);
		Plan plan = savePlan(42L, dogId, Long.MAX_VALUE);
		plan.complete(Instant.parse("2026-10-23T12:00:00Z"));
		flushAndClear();

		var result = finder.find(42L, dogId).orElseThrow();

		assertThat(result.plan().getId()).isEqualTo(plan.getId());
		assertThat(result.curriculums()).isEmpty();
	}

	@Test
	void 완료_시각이_같으면_ID가_큰_플랜을_반환한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		Instant time = Instant.parse("2026-10-23T12:00:00Z");
		savePlan(42L, dogId, curriculumId).complete(time);
		Plan latest = savePlan(42L, dogId, curriculumId);
		latest.complete(time);
		flushAndClear();

		assertThat(finder.find(42L, dogId).orElseThrow().plan().getId()).isEqualTo(latest.getId());
	}

	@Test
	void 다른_사용자_플랜과_교체된_플랜은_현재_플랜으로_반환하지_않는다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		savePlan(42L, dogId, curriculumId).replace();
		savePlan(43L, dogId, curriculumId);
		flushAndClear();

		assertThat(finder.find(42L, dogId)).isEmpty();
	}

	@Test
	void 일반_학습과_이전_플랜의_완료는_현재_플랜_진도에_반영하지_않는다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long lessonId = curriculum.getLessonIds().getFirst();
		LessonProgress progress = LessonProgress.start(42L, lessonId);
		progress.complete();
		entityManager.persist(progress);
		Plan previous = savePlan(42L, dogId, curriculumId);
		previous.completeLesson(lessonId);
		previous.replace();
		savePlan(42L, dogId, curriculumId);
		flushAndClear();

		var result = finder.find(42L, dogId).orElseThrow().curriculums().getFirst();

		assertThat(result.completedLessons()).isZero();
		assertThat(result.status()).isEqualTo(ProgressStatus.NOT_STARTED);
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(false);
	}

	@Test
	void 현재_플랜의_완료_기록으로_부분_완료와_전체_완료를_계산한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long firstLessonId = curriculum.getLessonIds().getFirst();
		Lesson second = LessonFixture.create(curriculum, "두 번째", 2, 5);
		entityManager.persist(second);
		Plan plan = savePlan(42L, dogId, curriculumId);
		plan.completeLesson(firstLessonId);
		flushAndClear();

		var partial = finder.find(42L, dogId).orElseThrow().curriculums().getFirst();

		assertThat(partial.totalLessons()).isEqualTo(2);
		assertThat(partial.completedLessons()).isEqualTo(1);
		assertThat(partial.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
		assertThat(partial.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(true, false);
		entityManager.find(Plan.class, plan.getId()).completeLesson(second.getId());
		flushAndClear();

		var completed = finder.find(42L, dogId).orElseThrow().curriculums().getFirst();

		assertThat(completed.completedLessons()).isEqualTo(2);
		assertThat(completed.status()).isEqualTo(ProgressStatus.COMPLETED);
	}

	@Test
	void 현재_목록에서_빠진_레슨은_완료_수에서_제외하고_추가된_레슨은_미완료로_표시한다() {
		Long dogId = saveDog(42L);
		Long curriculumId = saveCurriculum();
		flushAndClear();
		Curriculum curriculum = entityManager.find(Curriculum.class, curriculumId);
		Long removedLessonId = curriculum.getLessonIds().getFirst();
		Plan plan = savePlan(42L, dogId, curriculumId);
		plan.completeLesson(removedLessonId);
		entityManager.remove(entityManager.find(Lesson.class, removedLessonId));
		Lesson added = LessonFixture.create(curriculum, "새 레슨", 1, 5);
		entityManager.persist(added);
		flushAndClear();

		var result = finder.find(42L, dogId).orElseThrow().curriculums().getFirst();

		assertThat(result.totalLessons()).isEqualTo(1);
		assertThat(result.completedLessons()).isZero();
		assertThat(result.status()).isEqualTo(ProgressStatus.NOT_STARTED);
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::lessonId).containsExactly(added.getId());
		assertThat(result.lessons()).extracting(PlanLessonProgressResult::completed).containsExactly(false);
	}

	private Plan savePlan(Long userId, Long curriculumId) {
		return savePlan(userId, 1L, curriculumId);
	}

	private Plan savePlan(Long userId, Long dogId, Long curriculumId) {
		PlanCreateCommand command = new PlanCreateCommand(userId, dogId, "초코의 훈련 플랜", "목표");
		Plan plan = Plan.create(command, List.of(curriculumId));
		planRepository.flush();
		return planRepository.save(plan);
	}

	private Long saveDog(Long userId) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "보리", new Breed("믹스"), DogSex.MALE, null, null, null, null));
		entityManager.persist(dog);
		return dog.getId();
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
