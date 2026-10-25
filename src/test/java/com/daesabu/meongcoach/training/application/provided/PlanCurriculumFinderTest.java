package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.PlanLessonResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanCurriculumFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private PlanCurriculumFinder finder;

	@Autowired
	private TrainingCategoryRepository categoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonProgressRepository progressRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 일반_학습_기록과_관계없이_플랜_순서와_현재_레슨_순서를_반환한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum first = saveCurriculum(topic, 1, false);
		saveLesson(first);
		Curriculum second = saveCurriculum(topic, 2, false);
		Lesson later = lessonRepository.save(LessonFixture.create(second, "두 번째", 2, 5));
		Lesson earlier = lessonRepository.save(LessonFixture.create(second, "첫 번째", 1, 5));
		complete(USER_ID, earlier);
		complete(43L, later);
		flushAndClear();

		List<PlanCurriculumResult> results = finder.find(USER_ID, List.of(second.getId(), first.getId()));

		assertThat(results).extracting(PlanCurriculumResult::id).containsExactly(second.getId(), first.getId());
		PlanCurriculumResult result = results.getFirst();
		assertThat(result.title()).isEqualTo("훈련 2");
		assertThat(result.locked()).isFalse();
		assertThat(result.lessons()).extracting(PlanLessonResult::id).containsExactly(earlier.getId(), later.getId());
	}

	@Test
	void 유료_커리큘럼도_반환하고_이용권에_따라_잠금만_표시한다() {
		TrainingCategory category = categoryRepository.save(
				TrainingCategoryFixture.create("유료", 1, null, null, EntitlementType.PUPPY));
		Topic topic = saveTopic(category, 1);
		Curriculum premium = saveCurriculum(topic, 1, true);
		Curriculum free = saveCurriculum(topic, 2, false);
		saveLesson(free);
		complete(USER_ID, saveLesson(premium));
		flushAndClear();

		List<PlanCurriculumResult> results = finder.find(USER_ID, List.of(premium.getId(), free.getId()));

		assertThat(results).extracting(PlanCurriculumResult::locked).containsExactly(true, false);
		entityManager.persist(EntitlementFixture.create(
				USER_ID, EntitlementType.PUPPY, null));
		flushAndClear();
		assertThat(finder.find(USER_ID, List.of(premium.getId())).getFirst().locked()).isFalse();
	}

	@Test
	void 빈_목록이면_빈_결과를_반환한다() {
		assertThat(finder.find(USER_ID, List.of())).isEmpty();
		assertThat(finder.findLessonIds(List.of())).isEmpty();
	}

	@Test
	void 존재하지_않는_커리큘럼은_거절한다() {
		assertThatThrownBy(() -> finder.find(USER_ID, List.of(Long.MAX_VALUE)))
				.isInstanceOf(CurriculumNotFoundException.class);
		assertThatThrownBy(() -> finder.findLessonIds(List.of(Long.MAX_VALUE)))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	void 레슨이_없는_커리큘럼은_거절한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum curriculum = saveCurriculum(topic, 1, false);
		flushAndClear();

		assertThatThrownBy(() -> finder.find(USER_ID, List.of(curriculum.getId())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("커리큘럼에는 레슨이 하나 이상 있어야 합니다.");
	}

	@Test
	void 이용권과_학습_기록에_관계없이_커리큘럼과_레슨_순서대로_ID를_반환한다() {
		TrainingCategory category = categoryRepository.save(
				TrainingCategoryFixture.create("유료", 1, null, null, EntitlementType.PUPPY));
		Topic topic = saveTopic(category, 1);
		Curriculum first = saveCurriculum(topic, 1, true);
		Lesson firstLesson = saveLesson(first);
		Curriculum second = saveCurriculum(topic, 2, true);
		Lesson later = lessonRepository.save(LessonFixture.create(second, "두 번째", 2, 5));
		Lesson earlier = lessonRepository.save(LessonFixture.create(second, "첫 번째", 1, 5));
		Lesson sameOrder = lessonRepository.save(LessonFixture.create(second, "같은 순서", 1, 5));
		complete(USER_ID, earlier);
		flushAndClear();

		List<Long> lessonIds = finder.findLessonIds(List.of(second.getId(), first.getId()));

		assertThat(lessonIds).containsExactly(earlier.getId(), sameOrder.getId(), later.getId(), firstLesson.getId());
	}

	@Test
	void 레슨_ID_조회에서_뒤의_커리큘럼에_레슨이_없어도_거절한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum first = saveCurriculum(topic, 1, false);
		saveLesson(first);
		Curriculum empty = saveCurriculum(topic, 2, false);
		flushAndClear();

		assertThatThrownBy(() -> finder.findLessonIds(List.of(first.getId(), empty.getId())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("커리큘럼에는 레슨이 하나 이상 있어야 합니다.");
	}

	private TrainingCategory saveCategory(int order) {
		return categoryRepository.save(TrainingCategoryFixture.create("교육 " + order, order, null, null));
	}

	private Topic saveTopic(TrainingCategory category, int order) {
		return topicRepository.save(TopicFixture.create(category, "토픽 " + order, order, "설명", null, null));
	}

	private Curriculum saveCurriculum(Topic topic, int order, boolean premium) {
		return curriculumRepository.save(CurriculumFixture.create(topic, "훈련 " + order, order, null, null, premium));
	}

	private Lesson saveLesson(Curriculum curriculum) {
		return lessonRepository.save(LessonFixture.create(curriculum, "레슨", 1, 5));
	}

	private void complete(Long userId, Lesson lesson) {
		LessonProgress progress = LessonProgress.start(userId, lesson.getId());
		progress.complete();
		progressRepository.save(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
