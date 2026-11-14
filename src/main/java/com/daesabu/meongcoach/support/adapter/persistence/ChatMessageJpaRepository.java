package com.daesabu.meongcoach.support.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {
	List<ChatMessageEntity> findByConversationIdOrderByIdDesc(long conversationId, Pageable pageable);

	List<ChatMessageEntity> findByConversationIdAndIdGreaterThanOrderByIdAsc(
			long conversationId, long after, Pageable pageable);

	List<ChatMessageEntity> findByConversationIdAndIdLessThanOrderByIdDesc(
			long conversationId, long before, Pageable pageable);

	Optional<ChatMessageEntity> findByConversationIdAndSenderIdAndClientMessageId(
			long conversationId, long senderId, UUID clientMessageId);
}
