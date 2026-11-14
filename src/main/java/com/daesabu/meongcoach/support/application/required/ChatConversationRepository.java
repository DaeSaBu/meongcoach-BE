package com.daesabu.meongcoach.support.application.required;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
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
			INSERT INTO support_conversations (member_id, created_at, updated_at)
			VALUES (:memberId, :now, :now)
			ON CONFLICT (member_id) WHERE deleted_at IS NULL DO NOTHING
			""", nativeQuery = true)
	void insertIfAbsent(@Param("memberId") long memberId, @Param("now") LocalDateTime now);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE SupportConversation c SET c.updatedAt = :now WHERE c.id = :id")
	void recordMessage(@Param("id") long id, @Param("now") LocalDateTime now);

	Optional<ChatConversation> findByMemberId(long memberId);

	List<ChatConversation> findAllByOrderByUpdatedAtDescIdDesc(Pageable pageable);

	@Query("""
			SELECT c FROM SupportConversation c
			WHERE c.updatedAt < :updatedAt
			OR (c.updatedAt = :updatedAt AND c.id < :id)
			ORDER BY c.updatedAt DESC, c.id DESC
			""")
	List<ChatConversation> findPrevious(
			@Param("updatedAt") LocalDateTime updatedAt, @Param("id") long id, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChatConversation> findLockedById(long id);
}
