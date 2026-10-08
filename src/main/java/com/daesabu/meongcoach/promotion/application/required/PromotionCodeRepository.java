package com.daesabu.meongcoach.promotion.application.required;

import com.daesabu.meongcoach.promotion.domain.PromotionCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, Long> {

	/**
	 * 코드 문자열이 일치하는 프로모션 코드를 찾고 그 행에 쓰기 잠금(SELECT ... FOR UPDATE)을 건다. 같은 코드를 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잠금은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM PromotionCode p WHERE p.code = :code")
	Optional<PromotionCode> findByCodeForUpdate(String code);
}
