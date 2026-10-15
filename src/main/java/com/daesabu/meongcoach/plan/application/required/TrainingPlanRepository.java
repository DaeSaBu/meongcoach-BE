package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 맞춤 플랜 저장·조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

	/**
	 * 사용자의 해당 상태 플랜을 모두 조회한다. 없으면 빈 리스트를 반환한다.
	 */
	List<TrainingPlan> findByUserIdAndStatus(Long userId, PlanStatus status);

	/**
	 * 사용자에게 해당 상태의 플랜이 있는지 확인한다.
	 */
	boolean existsByUserIdAndStatus(Long userId, PlanStatus status);

	/**
	 * 플랜 ID와 소유자가 모두 일치할 때만 조회한다. 다른 사용자의 플랜이면 빈 Optional을 반환한다.
	 */
	Optional<TrainingPlan> findByIdAndUserId(Long id, Long userId);
}
