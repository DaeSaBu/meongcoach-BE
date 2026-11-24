package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.UserBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 사용자 차단 저장 리포지토리.
 */
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

	/**
	 * 회원이 대상 회원을 차단한 기록이 있는지 확인한다.
	 */
	boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

	/**
	 * 회원이 차단한 기록을 최근에 차단한 순서로 조회한다.
	 */
	List<UserBlock> findByBlockerIdOrderByIdDesc(Long blockerId);

	/**
	 * 회원이 대상 회원을 차단한 기록을 삭제한다. 기록이 없으면 아무것도 하지 않는다.
	 */
	void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 회원을 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잡은 락은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:blockerId)", nativeQuery = true)
	void lockByBlockerId(Long blockerId);
}
