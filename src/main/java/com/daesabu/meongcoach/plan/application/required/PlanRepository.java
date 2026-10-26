package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
	Optional<Plan> findByIdAndUserId(Long planId, Long userId);

	Optional<Plan> findByUserIdAndStatus(Long userId, PlanStatus status);

	Optional<Plan> findFirstByUserIdAndStatusOrderByCompletedAtDescIdDesc(Long userId, PlanStatus status);
}
