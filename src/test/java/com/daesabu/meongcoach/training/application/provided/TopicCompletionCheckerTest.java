package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
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
import org.junit.jupiter.api.BeforeEach;
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
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	private Long dogId;

	private Long otherUserDogId;

	@BeforeEach
	void setUp() {
		dogId = dogRepository.save(DogFixture.selected(USER_ID)).getId();
		otherUserDogId = dogRepository.save(DogFixture.selected(43L)).getId();
	}

	@Test
	void 모든_커리큘럼의_레슨을_완료하면_토픽이_완료다() {
		Topic topic = saveTopic();
		Lesson first = saveLesson(saveCurriculum(topic, 1), "앉아 1");
		Lesson second = saveLesson(saveCurriculum(topic, 2), "앉아 2");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, second.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, dogId, topic.getId())).isTrue();
	}

	@Test
	void 완료하지_않은_레슨이_남아_있으면_토픽이_완료가_아니다() {
		Topic topic = saveTopic();
		Lesson first = saveLesson(saveCurriculum(topic, 1), "앉아 1");
		saveLesson(saveCurriculum(topic, 2), "앉아 2");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, dogId, topic.getId())).isFalse();
	}

	@Test
	void 커리큘럼이_없는_토픽은_완료가_아니다() {
		Topic topic = saveTopic();
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, dogId, topic.getId())).isFalse();
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> topicCompletionChecker.isCompleted(USER_ID, dogId, 999L))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	void 본인이_모든_레슨을_완료한_토픽만_일괄_조회한다() {
		Topic completed = saveTopic();
		Lesson first = saveLesson(saveCurriculum(completed, 1), "완료");
		Topic partial = saveTopic();
		Lesson partialLesson = saveLesson(saveCurriculum(partial, 1), "일부 완료");
		saveLesson(saveCurriculum(partial, 2), "미완료");

		saveTopic();
		Topic otherUser = saveTopic();
		Lesson otherLesson = saveLesson(saveCurriculum(otherUser, 1), "다른 회원");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, partialLesson.getId());
		lessonCompleter.completeLesson(43L, otherUserDogId, otherLesson.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.findCompletedTopics(USER_ID, dogId))
				.extracting(com.daesabu.meongcoach.training.application.provided.dto.TopicResult::id)
				.containsExactly(completed.getId());
	}

	@Test
	void 같은_사용자의_다른_강아지가_완료한_토픽은_완료로_보지_않는다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Topic topic = saveTopic();
		Lesson lesson = saveLesson(saveCurriculum(topic, 1), "앉아");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, secondDogId, lesson.getId());
		flushAndClear();

		assertThat(topicCompletionChecker.isCompleted(USER_ID, dogId, topic.getId())).isFalse();
		assertThat(topicCompletionChecker.findCompletedTopics(USER_ID, dogId)).isEmpty();
		assertThat(topicCompletionChecker.isCompleted(USER_ID, secondDogId, topic.getId())).isTrue();
	}

	@Test
	void 레슨이_없는_커리큘럼이_있으면_완료_토픽_조회에서_예외를_던진다() {
		Topic topic = saveTopic();
		saveCurriculum(topic, 1);
		flushAndClear();

		assertThatThrownBy(() -> topicCompletionChecker.findCompletedTopics(USER_ID, dogId))
				.isInstanceOf(IllegalStateException.class);
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
