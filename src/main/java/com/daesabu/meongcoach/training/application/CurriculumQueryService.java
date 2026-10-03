package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.LessonResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
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

		List<CurriculumResult> curriculumResults = curriculums.stream()
				.map(curriculum -> toResult(curriculum, completedLessonIds))
				.toList();
		return new CurriculumListResult(topic.getId(), topic.getTitle(), curriculumResults);
	}

	@Override
	public CurriculumDetailResult findCurriculum(Long userId, Long curriculumId) {
		Curriculum curriculum = curriculumRepository.findById(curriculumId)
				.orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

		List<Long> lessonIds = curriculum.getLessonIds();
		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(userId, lessonIds);

		List<LessonResult> lessonResults = curriculum.getLessons().stream()
				.map(lesson -> toLessonResult(lesson, completedCounts.get(lesson.getId())))
				.toList();
		return new CurriculumDetailResult(curriculum.getId(), curriculum.getTopic().getId(), curriculum.getTitle(),
				curriculum.getSortOrder(), lessonResults);
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

	private CurriculumResult toResult(Curriculum curriculum, Set<Long> completedLessonIds) {
		int totalLessons = curriculum.getLessonsSize();
		int completedLessons = curriculum.countCompletedLessons(completedLessonIds);
		return new CurriculumResult(curriculum.getId(), curriculum.getTitle(), totalLessons, completedLessons,
				CurriculumStatus.of(totalLessons, completedLessons));
	}

	private LessonResult toLessonResult(Lesson lesson, int completedCount) {
		return new LessonResult(lesson.getId(), lesson.getTitle(), lesson.getSortOrder(), lesson.getEstimatedMinutes(),
				completedCount);
	}
}
