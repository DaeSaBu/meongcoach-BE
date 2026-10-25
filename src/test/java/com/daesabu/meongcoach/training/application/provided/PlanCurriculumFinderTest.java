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
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
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
	void 플랜_순서와_레슨_교육_순서를_유지하고_본인의_완료만_반영한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum first = saveCurriculum(topic, 1, false);
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
		assertThat(result.totalLessons()).isEqualTo(2);
		assertThat(result.completedLessons()).isEqualTo(1);
		assertThat(result.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
		assertThat(result.locked()).isFalse();
		assertThat(result.lessons()).extracting(PlanLessonResult::id).containsExactly(earlier.getId(), later.getId());
		assertThat(result.lessons()).extracting(PlanLessonResult::completed).containsExactly(true, false);
		assertThat(results.get(1).status()).isEqualTo(ProgressStatus.NOT_STARTED);
	}

	@Test
	void 유료_커리큘럼도_반환하고_이용권에_따라_잠금만_표시한다() {
		TrainingCategory category = categoryRepository.save(
				TrainingCategoryFixture.create("유료", 1, null, null, EntitlementType.PUPPY));
		Topic topic = saveTopic(category, 1);
		Curriculum premium = saveCurriculum(topic, 1, true);
		Curriculum free = saveCurriculum(topic, 2, false);
		complete(USER_ID, saveLesson(premium));
		flushAndClear();

		List<PlanCurriculumResult> results = finder.find(USER_ID, List.of(premium.getId(), free.getId()));

		assertThat(results).extracting(PlanCurriculumResult::locked).containsExactly(true, false);
		assertThat(results.getFirst().status()).isEqualTo(ProgressStatus.COMPLETED);
		entityManager.persist(EntitlementFixture.create(
				USER_ID, EntitlementType.PUPPY, null));
		flushAndClear();
		assertThat(finder.find(USER_ID, List.of(premium.getId())).getFirst().locked()).isFalse();
	}

	@Test
	void 빈_목록이면_빈_결과를_반환한다() {
		assertThat(finder.find(USER_ID, List.of())).isEmpty();
	}

	@Test
	void 존재하지_않는_커리큘럼은_거절한다() {
		assertThatThrownBy(() -> finder.find(USER_ID, List.of(Long.MAX_VALUE)))
				.isInstanceOf(CurriculumNotFoundException.class);
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
