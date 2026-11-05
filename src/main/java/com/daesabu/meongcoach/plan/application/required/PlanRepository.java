package com.daesabu.meongcoach.plan.application.required;

import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
	Optional<Plan> findByIdAndUserId(Long planId, Long userId);

	/**
	 * 사용자의 완료 플랜 중 가장 최근에 완료한 플랜을 조회한다.
	 *
	 * @deprecated 사용 중단 예정. 강아지별 조회로 이전한다.
	 */
	@Deprecated(forRemoval = true)
	Optional<Plan> findFirstByUserIdAndStatusOrderByCompletedAtDescIdDesc(Long userId, PlanStatus status);

	Optional<Plan> findByUserIdAndDogIdAndStatus(Long userId, Long dogId, PlanStatus status);

	Optional<Plan> findFirstByUserIdAndDogIdAndStatusOrderByCompletedAtDescIdDesc(Long userId, Long dogId, PlanStatus status);

	/**
	 * 사용자의 활성 플랜 중 가장 최근에 생성한 플랜을 조회한다.
	 *
	 * @deprecated 사용 중단 예정. 강아지별 조회로 이전한다.
	 */
	@Deprecated(forRemoval = true)
	Optional<Plan> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, PlanStatus status);
}
