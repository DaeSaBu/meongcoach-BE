package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.AiPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.CustomPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.AiPlanPreparationResult;
import com.daesabu.meongcoach.training.application.provided.dto.CustomPlanPreparationResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCompletion;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicQueryService implements TopicFinder, TopicCompletionChecker, CustomPlanPreparationFinder,
		AiPlanPreparationFinder {
	private final LessonProgressRepository lessonProgressRepository;
	private final TopicRepository topicRepository;

	@Override
	public AiPlanPreparationResult find(Long userId) {
		List<Topic> topics = topicRepository.findAll(TopicRepository.EDUCATION_ORDER);
		List<Long> lessonIds = topics.stream()
				.flatMap(topic -> topic.getAllLessonIds().stream())
				.toList();
		LessonProgresses progresses = findProgresses(userId, lessonIds);
		List<AiPlanCurriculumResult> curriculums = topics.stream()
				.flatMap(topic -> topic.getCurriculums().stream()
						.map(curriculum -> toAiPlanCurriculumResult(topic.getId(), topic.getTitle(), curriculum, progresses)))
				.toList();
		return new AiPlanPreparationResult(curriculums);
	}

	private AiPlanCurriculumResult toAiPlanCurriculumResult(Long topicId, String topicTitle, Curriculum curriculum,
			LessonProgresses progresses) {
		CurriculumCompletion completion = curriculum.completion(progresses);
		Long id = curriculum.getId();
		String title = curriculum.getTitle();
		String description = curriculum.getDescription();
		int totalLessons = curriculum.getLessonsSize();
		int completedLessons = completion.completedLessons();
		int estimatedMinutes = curriculum.getLessons().stream()
				.mapToInt(Lesson::getEstimatedMinutes)
				.sum();
		return new AiPlanCurriculumResult(id, topicId, topicTitle, title, description,
				totalLessons, completedLessons, estimatedMinutes);
	}

	@Override
	public CustomPlanPreparationResult find(Long userId, Collection<Long> topicIds) {
		List<Topic> topics = topicRepository.findAllByIdIn(topicIds, TopicRepository.EDUCATION_ORDER);
		validateTopicsExist(topicIds, topics);
		List<Long> lessonIds = topics.stream().flatMap(topic -> topic.getAllLessonIds().stream()).toList();
		LessonProgresses progresses = findProgresses(userId, lessonIds);
		List<Curriculum> orderedCurriculums = topics.stream()
				.flatMap(topic -> topic.getCurriculums().stream())
				.toList();
		orderedCurriculums.forEach(Curriculum::validateHasLessons);
		List<Long> curriculumIds = orderedCurriculums.stream()
				.dropWhile(curriculum -> curriculum.isCompleted(progresses))
				.map(Curriculum::getId)
				.toList();
		return new CustomPlanPreparationResult(curriculumIds);
	}

	@Override
	public List<TopicResult> findAllOrdered() {
		return topicRepository.findAll(TopicRepository.EDUCATION_ORDER).stream()
				.map(TopicResult::from)
				.toList();
	}

	@Override
	public List<TopicResult> findTopics(Collection<Long> topicIds) {
		List<Topic> topics = topicRepository.findAllById(topicIds);
		validateTopicsExist(topicIds, topics);
		return topics.stream()
				.map(TopicResult::from)
				.toList();
	}

	@Override
	public TopicResult findTopic(Long topicId) {
		Topic topic = loadTopic(topicId);
		return TopicResult.from(topic);
	}

	@Override
	public boolean isCompleted(Long userId, Long topicId) {
		Topic topic = loadTopic(topicId);
		LessonProgresses progresses = findProgresses(userId, topic.getAllLessonIds());
		return topic.isCompleted(progresses);
	}

	@Override
	public List<TopicResult> findCompletedTopics(Long userId) {
		List<Topic> topics = topicRepository.findAll(TopicRepository.EDUCATION_ORDER);
		if (topics.isEmpty()) {
			return List.of();
		}
		List<Long> lessonIds = topics.stream().flatMap(topic -> topic.getAllLessonIds().stream()).toList();
		LessonProgresses progresses = findProgresses(userId, lessonIds);
		return topics.stream()
				.filter(topic -> topic.isCompleted(progresses))
				.map(TopicResult::from)
				.toList();
	}

	private void validateTopicsExist(Collection<Long> topicIds, List<Topic> topics) {
		Set<Long> foundIds = topics.stream()
				.map(Topic::getId)
				.collect(Collectors.toSet());

		for (Long topicId : topicIds) {
			if (!foundIds.contains(topicId)) {
				throw new TopicNotFoundException(topicId);
			}
		}
	}

	private Topic loadTopic(Long topicId) {
		return topicRepository.findById(topicId).orElseThrow(() -> new TopicNotFoundException(topicId));
	}

	private LessonProgresses findProgresses(Long userId, List<Long> lessonIds) {
		List<LessonProgress> progresses = lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		return LessonProgresses.from(progresses);
	}

}
