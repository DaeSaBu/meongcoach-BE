package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
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
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
class TopicSeeder {

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonProgressUpdater lessonProgressUpdater;

	@Autowired
	private EntityManager entityManager;

	Topic completedTopic(Long userId, String title) {
		Topic topic = saveTopic(title);
		Lesson lesson = saveLesson(topic);
		flushAndClear();
		lessonProgressUpdater.updateCompletion(userId, lesson.getId());
		flushAndClear();
		return topic;
	}

	Topic incompleteTopic(String title) {
		Topic topic = saveTopic(title);
		saveLesson(topic);
		flushAndClear();
		return topic;
	}

	void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private Topic saveTopic(String title) {
		TrainingCategory category = trainingCategoryRepository.save(TrainingCategoryFixture.create("기본 교육", 1, null, null));
		return topicRepository.save(TopicFixture.create(category, title, 1, null, null, null));
	}

	private Lesson saveLesson(Topic topic) {
		Curriculum curriculum = curriculumRepository.save(CurriculumFixture.create(topic, title(topic), 1, null, null));
		return lessonRepository.save(LessonFixture.create(curriculum, title(topic) + " 레슨", 1, 5));
	}

	private String title(Topic topic) {
		return topic.getTitle() + " 1단계";
	}
}
