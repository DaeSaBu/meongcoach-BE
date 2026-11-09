package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.SupportReplyReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportReplyReceiptRepository extends JpaRepository<SupportReplyReceipt, String> {
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended('support-reply:' || :messageId, 0))", nativeQuery = true)
	int lock(@Param("messageId") String messageId);
}
