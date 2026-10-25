package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.PlanCompleter;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.training.application.provided.CurriculumCompletionChecker;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanCompletionService implements PlanCompleter {

	private final PlanRepository planRepository;
	private final CurriculumCompletionChecker curriculumCompletionChecker;

	@Override
	@Transactional
	public void complete(Long userId) {
		Optional<Plan> activePlan =
				planRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE);

		if (activePlan.isEmpty()) {
			return;
		}

		Plan plan = activePlan.get();
		boolean allCompleted =
				curriculumCompletionChecker.areAllCompleted(userId, plan.getCurriculumIds());

		if (!allCompleted) {
			return;
		}

		plan.complete(Instant.now());
	}
}
