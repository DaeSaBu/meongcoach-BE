package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.CustomPlanPreparationResult;
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
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CustomPlanPreparationFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private CustomPlanPreparationFinder finder;

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
	void 요청_순서와_관계없이_선택한_토픽의_커리큘럼을_교육_순서로_반환한다() {
		TrainingCategory laterCategory = saveCategory(2);
		Topic later = saveTopic(laterCategory, 1);
		Curriculum last = saveCurriculum(later, 1, false);
		saveLesson(last);
		TrainingCategory firstCategory = saveCategory(1);
		Topic second = saveTopic(firstCategory, 2);
		Curriculum third = saveCurriculum(second, 1, false);
		saveLesson(third);
		Topic first = saveTopic(firstCategory, 1);
		Curriculum secondCurriculum = saveCurriculum(first, 2, false);
		saveLesson(secondCurriculum);
		Curriculum firstCurriculum = saveCurriculum(first, 1, false);
		saveLesson(firstCurriculum);
		saveCurriculum(saveTopic(firstCategory, 3), 1, false);
		flushAndClear();

		CustomPlanPreparationResult result = finder.find(USER_ID, List.of(later.getId(), second.getId(), first.getId()));

		assertThat(result.curriculumIds())
				.containsExactly(firstCurriculum.getId(), secondCurriculum.getId(), third.getId(), last.getId());
	}

	@Test
	void 본인이_완료한_커리큘럼만_제외하고_일부_완료와_다른_사용자_완료는_포함한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum completed = saveCurriculum(topic, 1, false);
		complete(USER_ID, saveLesson(completed));
		Curriculum partial = saveCurriculum(topic, 2, false);
		complete(USER_ID, saveLesson(partial));
		saveLesson(partial);
		Curriculum otherUser = saveCurriculum(topic, 3, false);
		complete(43L, saveLesson(otherUser));
		flushAndClear();

		CustomPlanPreparationResult result = finder.find(USER_ID, List.of(topic.getId()));

		assertThat(result.curriculumIds()).containsExactly(partial.getId(), otherUser.getId());
	}

	@Test
	void 모두_완료하면_빈_커리큘럼_목록을_반환한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		complete(USER_ID, saveLesson(saveCurriculum(topic, 1, false)));
		flushAndClear();

		CustomPlanPreparationResult result = finder.find(USER_ID, List.of(topic.getId()));

		assertThat(result.curriculumIds()).isEmpty();
	}

	@Test
	void 이용권이_없어도_프리미엄_커리큘럼을_포함한다() {
		TrainingCategory category = categoryRepository.save(
				TrainingCategoryFixture.create("유료 교육", 1, null, null, EntitlementType.PUPPY));
		Topic topic = saveTopic(category, 1);
		Curriculum curriculum = saveCurriculum(topic, 1, true);
		saveLesson(curriculum);
		flushAndClear();

		CustomPlanPreparationResult result = finder.find(USER_ID, List.of(topic.getId()));

		assertThat(result.curriculumIds()).containsExactly(curriculum.getId());
	}

	@Test
	void 요청한_토픽_중_하나라도_없으면_거절한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		flushAndClear();

		assertThatThrownBy(() -> finder.find(USER_ID, List.of(topic.getId(), Long.MAX_VALUE)))
				.isInstanceOf(TopicNotFoundException.class);
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
