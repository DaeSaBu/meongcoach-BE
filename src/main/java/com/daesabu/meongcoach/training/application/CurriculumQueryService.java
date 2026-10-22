package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TopicSelectionRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicSelection;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements CurriculumFinder {
	private final TopicRepository topicRepository;
	private final CurriculumRepository curriculumRepository;
	private final TopicSelectionRepository topicSelectionRepository;
	private final LessonProgressRepository lessonProgressRepository;
	private final EntitlementChecker entitlementChecker;

	@Override
	public CurriculumListResult findCurriculums(Long userId) {
		Topic topic = findSelectedOrFirstTopic(userId);
		LessonProgresses progresses = findProgresses(userId, topic.getAllLessonIds());
		boolean hasEntitlement = hasEntitlement(userId, topic);
		return CurriculumListResult.of(topic, progresses, hasEntitlement);
	}

	@Override
	public CurriculumDetailResult findCurriculum(Long userId, Long curriculumId) {
		Curriculum curriculum = curriculumRepository.findById(curriculumId)
				.orElseThrow(() -> new CurriculumNotFoundException(curriculumId));
		curriculum.findRequiredEntitlementType()
				.ifPresent(type -> entitlementChecker.validateEntitlement(userId, type));
		LessonProgresses progresses = findProgresses(userId, curriculum.getLessonIds());
		return new CurriculumDetailResult(curriculum, progresses);
	}

	private Topic findSelectedOrFirstTopic(Long userId) {
		return topicSelectionRepository.findByUserId(userId)
				.map(TopicSelection::getTopicId)
				.flatMap(topicRepository::findById)
				.orElseGet(this::findFirstTopic);
	}

	private Topic findFirstTopic() {
		return topicRepository.findFirstBy(TopicRepository.EDUCATION_ORDER)
				.orElseThrow(TopicNotConfiguredException::new);
	}

	private boolean hasEntitlement(Long userId, Topic topic) {
		return topic.findRequiredEntitlementType()
				.map(type -> entitlementChecker.hasEntitlement(userId, type))
				.orElse(true);
	}

	private LessonProgresses findProgresses(Long userId, List<Long> lessonIds) {
		List<LessonProgress> progresses = lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		return LessonProgresses.from(progresses);
	}
}
