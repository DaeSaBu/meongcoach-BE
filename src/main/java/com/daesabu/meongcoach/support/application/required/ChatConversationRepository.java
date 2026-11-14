package com.daesabu.meongcoach.support.application.required;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
	@Modifying
	@Query(value = """
			INSERT INTO support_conversations (id, updated_at)
			VALUES (:id, current_timestamp) ON CONFLICT DO NOTHING
			""", nativeQuery = true)
	void insertIfAbsent(@Param("id") long id);

	List<ChatConversation> findAllByOrderByUpdatedAtDescIdDesc(Pageable pageable);

	@Query("""
			SELECT c FROM SupportConversation c
			WHERE c.updatedAt < :updatedAt
			OR (c.updatedAt = :updatedAt AND c.id < :id)
			ORDER BY c.updatedAt DESC, c.id DESC
			""")
	List<ChatConversation> findPrevious(
			@Param("updatedAt") Instant updatedAt, @Param("id") long id, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChatConversation> findLockedById(long id);
}
