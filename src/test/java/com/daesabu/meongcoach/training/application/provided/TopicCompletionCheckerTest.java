package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TopicCompletionCheckerTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private TopicCompletionChecker topicCompletionChecker;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 모든_커리큘럼의_레슨을_완료하면_토픽이_완료다() {
		Topic topic = saveTopic();
		Lesson first = saveLesson(saveCurriculum(topic, 1), "앉아 1");
		Lesson second = saveLesson(saveCurriculum(topic, 2), "앉아 2");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, first.getId());
		lessonCompleter.completeLesson(USER_ID, second.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, topic.getId())).isTrue();
	}

	@Test
	void 완료하지_않은_레슨이_남아_있으면_토픽이_완료가_아니다() {
		Topic topic = saveTopic();
		Lesson first = saveLesson(saveCurriculum(topic, 1), "앉아 1");
		saveLesson(saveCurriculum(topic, 2), "앉아 2");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, first.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, topic.getId())).isFalse();
	}

	@Test
	void 커리큘럼이_없는_토픽은_완료가_아니다() {
		Topic topic = saveTopic();
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, topic.getId())).isFalse();
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> topicCompletionChecker.isCompleted(USER_ID, 999L))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	void 본인이_모든_레슨을_완료한_토픽만_일괄_조회한다() {
		Topic completed = saveTopic();
		Lesson first = saveLesson(saveCurriculum(completed, 1), "완료");
		Topic partial = saveTopic();
		Lesson partialLesson = saveLesson(saveCurriculum(partial, 1), "일부 완료");
		saveLesson(saveCurriculum(partial, 2), "미완료");
		Topic emptyCurriculum = saveTopic();
		saveCurriculum(emptyCurriculum, 1);
		saveTopic();
		Topic otherUser = saveTopic();
		Lesson otherLesson = saveLesson(saveCurriculum(otherUser, 1), "다른 회원");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, first.getId());
		lessonCompleter.completeLesson(USER_ID, partialLesson.getId());
		lessonCompleter.completeLesson(43L, otherLesson.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.findCompletedTopics(USER_ID))
				.extracting(com.daesabu.meongcoach.training.application.provided.dto.TopicResult::id)
				.containsExactly(completed.getId());
	}

	private Topic saveTopic() {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create("기본 교육", 1, null, null));
		return topicRepository.save(TopicFixture.create(category, "앉아", 1, null, null, null));
	}

	private Curriculum saveCurriculum(Topic topic, int sortOrder) {
		return curriculumRepository.save(CurriculumFixture.create(topic, "앉아 " + sortOrder + "단계", sortOrder, null, null));
	}

	private Lesson saveLesson(Curriculum curriculum, String title) {
		return lessonRepository.save(LessonFixture.create(curriculum, title, 1, 5));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
