package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

	List<Entitlement> findAllByUserId(Long userId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 회원을 잠근 다른 트랜잭션이 끝날 때까지 기다리고,
	 * 잡은 락은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:userId)", nativeQuery = true)
	Integer lockByUserId(Long userId);
}
