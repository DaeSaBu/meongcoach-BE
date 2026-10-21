package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicCompletionService implements TopicCompletionChecker {
	private final TopicRepository topicRepository;
	private final LessonProgressFinder lessonProgressFinder;

	@Override
	public boolean isCompleted(Long userId, Long topicId) {
		Topic topic = topicRepository.findById(topicId)
				.orElseThrow(() -> new TopicNotFoundException(topicId));
		List<Long> lessonIds = topic.getAllLessonIds();
		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(userId, lessonIds);
		return topic.isCompleted(completedLessonIds);
	}

	@Override
	public List<TopicResult> findCompletedTopics(Long userId) {
		List<Topic> topics = topicRepository.findAllOrdered();
		List<Long> lessonIds = topics.stream().flatMap(topic -> topic.getAllLessonIds().stream()).toList();
		if (lessonIds.isEmpty()) {
			return List.of();
		}
		Set<Long> completedLessonIds = lessonProgressFinder.findCompletedLessonIds(userId, lessonIds);
		return topics.stream()
				.filter(topic -> topic.isCompleted(completedLessonIds))
				.map(TopicResult::from)
				.toList();
	}

}
