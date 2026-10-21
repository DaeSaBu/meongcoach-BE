package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
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
public class TopicQueryService implements TopicFinder, TopicCompletionChecker {
	private final LessonProgressRepository lessonProgressRepository;
	private final TopicRepository topicRepository;

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
