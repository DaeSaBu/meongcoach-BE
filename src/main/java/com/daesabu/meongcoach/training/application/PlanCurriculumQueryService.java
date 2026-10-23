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
import java.util.function.Function;
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
		Map<Long, Curriculum> byId = curriculumRepository.findAllById(curriculumIds).stream()
				.collect(Collectors.toMap(Curriculum::getId, Function.identity()));
		List<Curriculum> curriculums = curriculumIds.stream().map(id -> requireCurriculum(byId, id)).toList();
		List<Long> lessonIds = curriculums.stream().flatMap(curriculum -> curriculum.getLessonIds().stream()).toList();
		List<LessonProgress> lessonProgresses = lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds);
		LessonProgresses progresses = LessonProgresses.from(lessonProgresses);
		Set<EntitlementType> activeTypes = entitlementChecker.findActiveTypes(userId);
		return curriculums.stream()
				.map(curriculum -> PlanCurriculumResult.of(curriculum, progresses,
						curriculum.findRequiredEntitlementType().map(activeTypes::contains).orElse(true)))
				.toList();
	}

	private Curriculum requireCurriculum(Map<Long, Curriculum> byId, Long id) {
		Curriculum curriculum = byId.get(id);
		if (curriculum == null) {
			throw new CurriculumNotFoundException(id);
		}
		return curriculum;
	}
}
