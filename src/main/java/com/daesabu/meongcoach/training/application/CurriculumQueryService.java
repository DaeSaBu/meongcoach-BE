package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements CurriculumFinder {
	private final TopicRepository topicRepository;
	private final CurriculumRepository curriculumRepository;
	private final TopicProgressFinder topicProgressFinder;
	private final LessonProgressFinder lessonProgressFinder;

	@Override
	public CurriculumListResult findCurriculums(Long userId) {
		Topic topic = findLatestOrFirstTopic(userId);

		List<Curriculum> curriculums = curriculumRepository.findAllByTopicId(topic.getId());
		Set<Long> completedLessonIds = findCompletedLessonIds(userId, curriculums);

		return new CurriculumListResult(topic, curriculums, completedLessonIds);
	}

	@Override
	public CurriculumDetailResult findCurriculum(Long userId, Long curriculumId) {
		Curriculum curriculum = findCurriculum(curriculumId);

		List<Long> lessonIds = curriculum.getLessonIds();
		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(userId, lessonIds);

		return new CurriculumDetailResult(curriculum, completedCounts);
	}

	private Curriculum findCurriculum(Long curriculumId) {
		return curriculumRepository.findById(curriculumId)
				.orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
	}

	private Topic findLatestOrFirstTopic(Long userId) {
		return topicProgressFinder.findLatestTopicId(userId)
				.flatMap(topicRepository::findById)
				.orElseGet(this::findFirstTopic);
	}

	private Topic findFirstTopic() {
		return topicRepository.findFirstTopic()
				.orElseThrow(TopicNotConfiguredException::new);
	}

	private Set<Long> findCompletedLessonIds(Long userId, List<Curriculum> curriculums) {
		List<Long> lessonIds = curriculums.stream()
				.flatMap(curriculum -> curriculum.getLessonIds().stream())
				.toList();
		return lessonProgressFinder.findCompletedLessonIds(userId, lessonIds);
	}
}
