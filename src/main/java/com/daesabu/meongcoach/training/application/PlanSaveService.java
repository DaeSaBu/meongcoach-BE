package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.required.PlanRepository;
import com.daesabu.meongcoach.training.domain.Plan;
import com.daesabu.meongcoach.training.domain.PlanStatus;
import com.daesabu.meongcoach.training.domain.exception.PlanCreationConflictException;
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
		planRepository.findByUserIdAndDogIdAndStatus(plan.getUserId(), plan.getDogId(), PlanStatus.ACTIVE).ifPresent(Plan::replace);
		try {
			planRepository.flush();
			return planRepository.saveAndFlush(plan);
		} catch (DataIntegrityViolationException exception) {
			throw new PlanCreationConflictException(exception);
		}
	}
}
