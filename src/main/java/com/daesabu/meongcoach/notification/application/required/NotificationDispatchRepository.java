package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, String> {
	/**
	 * 예약 키의 트랜잭션 잠금을 얻으면 true를 반환한다. 잠금은 커밋·롤백 시 해제된다.
	 */
	@Query(value = "SELECT pg_try_advisory_xact_lock(hashtextextended('notification-dispatch:' || :key, 0))", nativeQuery = true)
	boolean tryLock(String key);
}
