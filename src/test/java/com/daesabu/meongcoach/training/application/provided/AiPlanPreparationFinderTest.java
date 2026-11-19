package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanPreparationResult;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AiPlanPreparationFinderTest {

	private static final Long USER_ID = 42L;

	private static final Long DOG_ID = 7L;

	private static final Long OTHER_DOG_ID = 8L;

	@Autowired
	private AiPlanPreparationFinder finder;

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
	void 전체_커리큘럼을_교육_순서로_반환한다() {
		Topic later = saveTopic(saveCategory(2), 1);
		Curriculum last = saveCurriculum(later, 1);
		saveLesson(last);
		TrainingCategory firstCategory = saveCategory(1);
		Topic secondTopic = saveTopic(firstCategory, 2);
		Curriculum third = saveCurriculum(secondTopic, 1);
		saveLesson(third);
		Topic firstTopic = saveTopic(firstCategory, 1);
		Curriculum second = saveCurriculum(firstTopic, 2);
		saveLesson(second);
		Curriculum first = saveCurriculum(firstTopic, 1);
		saveLesson(first);
		flushAndClear();

		AiPlanPreparationResult result = finder.find(USER_ID, DOG_ID);

		assertThat(result.curriculums()).extracting(AiPlanCurriculumResult::id)
				.containsExactly(first.getId(), second.getId(), third.getId(), last.getId());
		assertThat(result.curriculums()).extracting(AiPlanCurriculumResult::topicId)
				.containsExactly(firstTopic.getId(), firstTopic.getId(), secondTopic.getId(), later.getId());
	}

	@Test
	void 완료한_커리큘럼도_포함하고_본인의_완료_레슨_수와_전체_시간을_반환한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum completed = saveCurriculum(topic, 1);
		complete(USER_ID, DOG_ID, saveLesson(completed));
		Curriculum partial = saveCurriculum(topic, 2);
		Lesson first = saveLesson(partial);
		LessonProgress repeated = LessonProgress.start(USER_ID, DOG_ID, first.getId());
		repeated.complete();
		repeated.complete();
		progressRepository.save(repeated);
		Lesson second = lessonRepository.save(LessonFixture.create(partial, "두 번째 레슨", 2, 7));
		complete(43L, DOG_ID, second);
		flushAndClear();

		AiPlanPreparationResult result = finder.find(USER_ID, DOG_ID);

		assertThat(result.curriculums()).containsExactly(
				new AiPlanCurriculumResult(completed.getId(), topic.getId(), topic.getTitle(), completed.getTitle(),
						completed.getDescription(), 1, 1),
				new AiPlanCurriculumResult(partial.getId(), topic.getId(), topic.getTitle(), partial.getTitle(),
						partial.getDescription(), 2, 1));
	}

	@Test
	void 같은_사용자의_다른_강아지가_완료한_레슨은_세지_않는다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		Curriculum curriculum = saveCurriculum(topic, 1);
		Lesson lesson = saveLesson(curriculum);
		complete(USER_ID, OTHER_DOG_ID, lesson);
		flushAndClear();

		AiPlanPreparationResult result = finder.find(USER_ID, DOG_ID);

		assertThat(result.curriculums()).extracting(AiPlanCurriculumResult::completedLessons)
				.containsExactly(0);
	}

	@Test
	void 커리큘럼이_없으면_빈_목록을_반환한다() {
		saveTopic(saveCategory(1), 1);
		flushAndClear();

		assertThat(finder.find(USER_ID, DOG_ID).curriculums()).isEmpty();
	}

	@Test
	void 레슨이_없는_커리큘럼이_있으면_거절한다() {
		Topic topic = saveTopic(saveCategory(1), 1);
		saveLesson(saveCurriculum(topic, 1));
		saveCurriculum(topic, 2);
		flushAndClear();

		assertThatThrownBy(() -> finder.find(USER_ID, DOG_ID)).isInstanceOf(IllegalStateException.class);
	}

	private TrainingCategory saveCategory(int order) {
		return categoryRepository.save(TrainingCategoryFixture.create("교육 " + order, order, null, null));
	}

	private Topic saveTopic(TrainingCategory category, int order) {
		return topicRepository.save(TopicFixture.create(category, "토픽 " + order, order, "설명", null, null));
	}

	private Curriculum saveCurriculum(Topic topic, int order) {
		return curriculumRepository.save(CurriculumFixture.create(topic, "훈련 " + order, order, null, null));
	}

	private Lesson saveLesson(Curriculum curriculum) {
		return lessonRepository.save(LessonFixture.create(curriculum, "레슨", 1, 5));
	}

	private void complete(Long userId, Long dogId, Lesson lesson) {
		LessonProgress progress = LessonProgress.start(userId, dogId, lesson.getId());
		progress.complete();
		progressRepository.save(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
