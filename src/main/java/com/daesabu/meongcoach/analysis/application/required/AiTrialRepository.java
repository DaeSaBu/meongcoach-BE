package com.daesabu.meongcoach.analysis.application.required;

import com.daesabu.meongcoach.analysis.domain.AiTrial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 사용자별 AI 분석 체험 횟수 저장 리포지토리.
 */
public interface AiTrialRepository extends JpaRepository<AiTrial, Long> {

	/**
	 * 사용자의 체험 횟수 행을 찾는다.
	 */
	Optional<AiTrial> findByUserId(Long userId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 회원을 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잡은 락은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:userId)", nativeQuery = true)
	void lockByUserId(Long userId);
}
