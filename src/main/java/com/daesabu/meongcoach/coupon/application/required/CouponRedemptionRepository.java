package com.daesabu.meongcoach.coupon.application.required;

import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 쿠폰 사용 기록 저장 리포지토리.
 */
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

	/**
	 * 사용자가 쿠폰을 사용한 기록이 있는지 확인한다.
	 */
	boolean existsByCouponIdAndUserId(Long couponId, Long userId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 회원을 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잡은 락은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:userId)", nativeQuery = true)
	void lockByUserId(Long userId);
}
