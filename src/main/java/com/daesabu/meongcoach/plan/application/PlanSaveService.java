package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PlanSaveService {

	private final PlanRepository planRepository;

	@Transactional
	public Plan save(Plan plan) {
		planRepository.findByUserIdAndStatus(plan.getUserId(), PlanStatus.ACTIVE).ifPresent(Plan::replace);
		try {
			planRepository.flush();
			return planRepository.saveAndFlush(plan);
		} catch (DataIntegrityViolationException exception) {
			throw new PlanCreationConflictException(exception);
		}
	}
}
