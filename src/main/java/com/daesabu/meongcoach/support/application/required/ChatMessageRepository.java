package com.daesabu.meongcoach.support.application.required;

import com.daesabu.meongcoach.support.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	Optional<ChatMessage> findByIdAndConversationId(long id, long conversationId);

	List<ChatMessage> findByConversationIdOrderByIdDesc(long conversationId, Pageable pageable);

	List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(
			long conversationId, long after, Pageable pageable);

	List<ChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(
			long conversationId, long before, Pageable pageable);

	Optional<ChatMessage> findByConversationIdAndSenderIdAndClientMessageId(
			long conversationId, long senderId, UUID clientMessageId);
}
