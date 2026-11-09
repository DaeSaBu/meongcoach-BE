package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicProgressQueryService implements TopicProgressFinder {

	private final TopicRepository topicRepository;
	private final LessonProgressRepository lessonProgressRepository;

	@Override
	public List<TopicProgressResult> findAll(Long userId) {
		List<Topic> topics = topicRepository.findAll(TopicRepository.EDUCATION_ORDER).stream()
				.filter(topic -> !topic.getAllLessonIds().isEmpty())
				.toList();
		List<Long> lessonIds = topics.stream()
				.flatMap(topic -> topic.getAllLessonIds().stream())
				.toList();
		LessonProgresses progresses = LessonProgresses.from(
				lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds));
		return topics.stream()
				.map(topic -> toResult(topic, progresses))
				.toList();
	}

	private TopicProgressResult toResult(Topic topic, LessonProgresses progresses) {
		List<Long> lessonIds = topic.getAllLessonIds();
		int completedLessons = progresses.countCompletedIn(lessonIds);
		LocalDateTime lastStudiedAt = progresses.findLastCompletedAt(lessonIds).orElse(null);
		Long nextCurriculumId = findNextCurriculumId(topic, progresses);
		TrainingCategory category = topic.getTrainingCategory();
		return new TopicProgressResult(topic.getId(), topic.getTitle(), topic.getDescription(), topic.getIconUrl(),
				topic.getThumbnailUrl(), category.getId(), category.getTitle(), category.getSortOrder(), lessonIds.size(),
				completedLessons, nextCurriculumId, lastStudiedAt);
	}

	private Long findNextCurriculumId(Topic topic, LessonProgresses progresses) {
		List<Curriculum> curriculums = topic.getCurriculums().stream()
				.filter(curriculum -> curriculum.getLessonsSize() > 0)
				.toList();
		return curriculums.stream()
				.filter(curriculum -> !curriculum.isCompleted(progresses))
				.findFirst()
				.orElse(curriculums.getFirst())
				.getId();
	}
}
