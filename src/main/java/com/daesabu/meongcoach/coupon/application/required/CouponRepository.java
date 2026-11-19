package com.daesabu.meongcoach.coupon.application.required;

import com.daesabu.meongcoach.coupon.domain.Coupon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 쿠폰 저장 리포지토리.
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

	/**
	 * 저장된 코드와 정확히 일치하는 쿠폰을 찾는다.
	 */
	Optional<Coupon> findByCode(String code);
}
