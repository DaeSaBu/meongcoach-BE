package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.LessonResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements CurriculumFinder {

	private final TopicRepository topicRepository;

	private final CurriculumRepository curriculumRepository;

	private final LessonRepository lessonRepository;

	private final TopicProgressFinder topicProgressFinder;

	private final LessonProgressFinder lessonProgressFinder;

	@Override
	public CurriculumListResult findCurriculums(Long userId) {
		Topic topic = resolveTopic(userId);

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());
		Map<Long, List<Lesson>> lessonsByCurriculumId = groupLessonsByCurriculumId(curriculums);
		Set<Long> completedLessonIds = findCompletedLessonIds(userId, lessonsByCurriculumId);

		List<CurriculumResult> curriculumResults = curriculums.stream()
				.map(curriculum -> toResult(curriculum,
						lessonsByCurriculumId.getOrDefault(curriculum.getId(), List.of()), completedLessonIds))
				.toList();
		return new CurriculumListResult(topic.getId(), topic.getTitle(), curriculumResults);
	}

	@Override
	public CurriculumDetailResult findCurriculum(Long userId, Long curriculumId) {
		Curriculum curriculum = curriculumRepository.findById(curriculumId)
				.orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdOrderBySortOrderAscIdAsc(curriculumId);
		List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(userId, lessonIds);

		List<LessonResult> lessonResults = lessons.stream()
				.map(lesson -> toLessonResult(lesson, completedCounts.get(lesson.getId())))
				.toList();
		return new CurriculumDetailResult(curriculum.getId(), curriculum.getTopic().getId(), curriculum.getTitle(),
				curriculum.getSortOrder(), lessonResults);
	}

	private Topic resolveTopic(Long userId) {
		return topicProgressFinder.findLatestTopicId(userId)
				.flatMap(topicRepository::findById)
				.orElseGet(this::findFirstTopic);
	}

	private Topic findFirstTopic() {
		return topicRepository.findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc()
				.orElseThrow(TopicNotConfiguredException::new);
	}

	private Map<Long, List<Lesson>> groupLessonsByCurriculumId(List<Curriculum> curriculums) {
		List<Long> curriculumIds = curriculums.stream().map(Curriculum::getId).toList();
		return lessonRepository.findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(curriculumIds).stream()
				.collect(Collectors.groupingBy(lesson -> lesson.getCurriculum().getId()));
	}

	private Set<Long> findCompletedLessonIds(Long userId, Map<Long, List<Lesson>> lessonsByCurriculumId) {
		List<Long> lessonIds = lessonsByCurriculumId.values().stream()
				.flatMap(List::stream)
				.map(Lesson::getId)
				.toList();
		return lessonProgressFinder.findCompletedLessonIds(userId, lessonIds);
	}

	private CurriculumResult toResult(Curriculum curriculum, List<Lesson> lessons, Set<Long> completedLessonIds) {
		int totalLessons = lessons.size();
		int completedLessons = (int) lessons.stream()
				.map(Lesson::getId)
				.filter(completedLessonIds::contains)
				.count();
		return new CurriculumResult(curriculum.getId(), curriculum.getTitle(), totalLessons, completedLessons,
				CurriculumStatus.of(totalLessons, completedLessons));
	}

	private LessonResult toLessonResult(Lesson lesson, int completedCount) {
		return new LessonResult(lesson.getId(), lesson.getTitle(), lesson.getSortOrder(), lesson.getEstimatedMinutes(),
				completedCount);
	}
}
