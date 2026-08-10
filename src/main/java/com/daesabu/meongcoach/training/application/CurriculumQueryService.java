package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.progress.application.provided.TopicEntryFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailView;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListView;
import com.daesabu.meongcoach.training.application.provided.CurriculumView;
import com.daesabu.meongcoach.training.application.provided.LessonView;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커리큘럼 리스트·세부 조회 서비스. 레슨과 진행도를 각각 한 번에 읽어 커리큘럼·레슨 수와 무관하게 쿼리 수를 상수로 유지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements CurriculumFinder {

	private final TopicRepository topicRepository;

	private final CurriculumRepository curriculumRepository;

	private final LessonRepository lessonRepository;

	private final TopicEntryFinder topicEntryFinder;

	private final LessonProgressFinder lessonProgressFinder;

	private final UserProfileFinder userProfileFinder;

	@Override
	public CurriculumListView findCurriculums(Long userId) {
		Topic topic = resolveTopic(userId);

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());
		Map<Long, List<Lesson>> lessonsByCurriculumId = groupLessonsByCurriculumId(curriculums);
		Set<Long> completedLessonIds = findCompletedLessonIds(userId, lessonsByCurriculumId);

		List<CurriculumView> curriculumViews = curriculums.stream()
				.map(curriculum -> toView(curriculum,
						lessonsByCurriculumId.getOrDefault(curriculum.getId(), List.of()), completedLessonIds))
				.toList();
		String profileImageUrl = userProfileFinder.findProfileImageUrl(userId);
		return new CurriculumListView(topic.getId(), topic.getTitle(), profileImageUrl, curriculumViews);
	}

	@Override
	public CurriculumDetailView findCurriculum(Long userId, Long curriculumId) {
		Curriculum curriculum = curriculumRepository.findById(curriculumId)
				.orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdOrderBySortOrderAscIdAsc(curriculumId);
		List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
		Map<Long, Integer> completedCounts = lessonProgressFinder.findCompletedCounts(userId, lessonIds);

		List<LessonView> lessonViews = lessons.stream()
				.map(lesson -> toLessonView(lesson, completedCounts.get(lesson.getId())))
				.toList();
		// 토픽은 지연 로딩 프록시의 id만 읽어 추가 쿼리 없이 얻는다
		return new CurriculumDetailView(curriculum.getId(), curriculum.getTopic().getId(), curriculum.getTitle(),
				curriculum.getSortOrder(), lessonViews);
	}

	// 진입 기록이 없거나 기록된 토픽이 더 이상 없으면 카테고리·토픽 정렬 순서 기준 첫 토픽으로 폴백한다
	private Topic resolveTopic(Long userId) {
		return topicEntryFinder.findLatestEnteredTopicId(userId)
				.flatMap(topicRepository::findById)
				.orElseGet(this::findFirstTopic);
	}

	private Topic findFirstTopic() {
		return topicRepository.findFirstByOrderByTrainingCategory_SortOrderAscSortOrderAscIdAsc()
				.orElseThrow(TopicNotConfiguredException::new);
	}

	// 레슨을 커리큘럼 id IN 조건으로 한 번에 읽어 커리큘럼별로 나눈다
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

	private CurriculumView toView(Curriculum curriculum, List<Lesson> lessons, Set<Long> completedLessonIds) {
		int totalLessons = lessons.size();
		int completedLessons = (int) lessons.stream()
				.map(Lesson::getId)
				.filter(completedLessonIds::contains)
				.count();
		return new CurriculumView(curriculum.getId(), curriculum.getTitle(), totalLessons, completedLessons,
				CurriculumStatus.of(totalLessons, completedLessons));
	}

	private LessonView toLessonView(Lesson lesson, int completedCount) {
		return new LessonView(lesson.getId(), lesson.getTitle(), lesson.getSortOrder(), lesson.getEstimatedMinutes(),
				completedCount);
	}
}
