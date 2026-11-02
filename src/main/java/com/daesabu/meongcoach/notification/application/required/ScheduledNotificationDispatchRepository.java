package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.ScheduledNotificationDispatch;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledNotificationDispatchRepository extends JpaRepository<ScheduledNotificationDispatch, Long> {
	boolean existsByScheduledAt(Instant scheduledAt);

	Optional<ScheduledNotificationDispatch> findByScheduledAt(Instant scheduledAt);

	/**
	 * 예약 키의 트랜잭션 잠금을 얻을 때까지 기다린다. 잠금은 커밋·롤백 시 해제된다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended('notification-dispatch:' || :key, 0))", nativeQuery = true)
	int lock(String key);
}
