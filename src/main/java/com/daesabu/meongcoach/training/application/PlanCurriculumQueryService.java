package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
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
public class PlanCurriculumQueryService implements PlanCurriculumFinder {

	private final CurriculumRepository curriculumRepository;
	private final LessonProgressRepository lessonProgressRepository;
	private final EntitlementChecker entitlementChecker;

	@Override
	public List<PlanCurriculumResult> find(Long userId, List<Long> curriculumIds) {
		if (curriculumIds.isEmpty()) {
			return List.of();
		}
		Map<Long, Curriculum> curriculumMap = curriculumRepository.findAllById(curriculumIds).stream()
				.collect(Collectors.toMap(Curriculum::getId, curriculum -> curriculum));
		List<Curriculum> orderedCurriculums = curriculumIds.stream()
				.map(curriculumId -> findCurriculumOrThrow(curriculumMap, curriculumId))
				.toList();
		List<Long> lessonIds = orderedCurriculums.stream().flatMap(curriculum -> curriculum.getLessonIds().stream()).toList();
		List<LessonProgress> lessonProgresses = lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		LessonProgresses progresses = LessonProgresses.from(lessonProgresses);
		Set<EntitlementType> activeTypes = entitlementChecker.findActiveTypes(userId);
		return orderedCurriculums.stream()
				.map(curriculum -> PlanCurriculumResult.of(curriculum, progresses,
						curriculum.findRequiredEntitlementType().map(activeTypes::contains).orElse(true)))
				.toList();
	}

	private Curriculum findCurriculumOrThrow(Map<Long, Curriculum> curriculumMap, Long curriculumId) {
		Curriculum curriculum = curriculumMap.get(curriculumId);
		if (curriculum == null) {
			throw new CurriculumNotFoundException(curriculumId);
		}
		return curriculum;
	}
}
