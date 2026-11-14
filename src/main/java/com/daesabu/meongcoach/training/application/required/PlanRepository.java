package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Plan;
import com.daesabu.meongcoach.training.domain.PlanStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
	Optional<Plan> findByIdAndUserId(Long planId, Long userId);

	Optional<Plan> findByUserIdAndDogIdAndStatus(Long userId, Long dogId, PlanStatus status);

	Optional<Plan> findFirstByUserIdAndDogIdAndStatusOrderByCompletedAtDescIdDesc(Long userId, Long dogId, PlanStatus status);
}
