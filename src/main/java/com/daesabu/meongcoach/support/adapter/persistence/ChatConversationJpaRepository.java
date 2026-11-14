package com.daesabu.meongcoach.support.adapter.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ChatConversationJpaRepository extends JpaRepository<ChatConversationEntity, Long> {
	@Modifying
	@Query(value = """
			INSERT INTO support_conversations (id, updated_at)
			VALUES (:id, current_timestamp) ON CONFLICT DO NOTHING
			""", nativeQuery = true)
	void insertIfAbsent(@Param("id") long id);

	List<ChatConversationEntity> findAllByOrderByUpdatedAtDescIdDesc(Pageable pageable);

	@Query("""
			SELECT c FROM SupportConversation c
			WHERE c.updatedAt < (SELECT p.updatedAt FROM SupportConversation p WHERE p.id = :before)
			OR (c.updatedAt = (SELECT p.updatedAt FROM SupportConversation p WHERE p.id = :before) AND c.id < :before)
			ORDER BY c.updatedAt DESC, c.id DESC
			""")
	List<ChatConversationEntity> findPrevious(@Param("before") long before, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChatConversationEntity> findLockedById(long id);
}
