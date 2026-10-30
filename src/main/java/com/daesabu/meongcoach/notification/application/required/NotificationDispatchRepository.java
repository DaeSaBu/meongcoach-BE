package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, String> {
	@Modifying
	@Query(value = "INSERT INTO notification_dispatches (id, created_at) VALUES (:id, :now) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
	int claim(String id, Instant now);
}
