package com.daesabu.meongcoach.report.application.required;

import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 신고 저장 리포지토리.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

	/**
	 * 회원이 대상을 신고한 기록이 있는지 확인한다.
	 */
	boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 회원을 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잡은 락은 현재 트랜잭션이 커밋·롤백될 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:reporterId)", nativeQuery = true)
	void lockByReporterId(Long reporterId);
}
