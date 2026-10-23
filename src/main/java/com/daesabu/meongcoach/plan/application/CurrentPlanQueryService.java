package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.training.application.provided.PlanCurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.dto.PlanCurriculumResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentPlanQueryService implements CurrentPlanFinder {

	private final PlanRepository planRepository;
	private final PlanCurriculumFinder planCurriculumFinder;

	@Override
	public Optional<CurrentPlanResult> find(Long userId) {
		return planRepository.findByUserIdAndStatus(userId, PlanStatus.ACTIVE)
				.or(() -> planRepository.findFirstByUserIdAndStatusOrderByCompletedAtDescIdDesc(userId, PlanStatus.COMPLETED))
				.map(plan -> toResult(userId, plan));
	}

	private CurrentPlanResult toResult(Long userId, Plan plan) {
		List<PlanCurriculumResult> curriculums = planCurriculumFinder.find(userId, plan.getCurriculumIds());
		return new CurrentPlanResult(plan, curriculums);
	}
}
