package com.daesabu.meongcoach.support.application.required;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository {
	ChatConversation open(long userId);

	List<ChatConversation> conversations(long before, Instant beforeUpdatedAt, int limit);

	boolean exists(long conversationId);

	void lock(long conversationId);

	List<ChatMessage> latestMessages(long conversationId, int limit);

	List<ChatMessage> newMessages(long conversationId, long after, int limit);

	List<ChatMessage> previousMessages(long conversationId, long before, int limit);

	Optional<ChatMessage> find(long conversationId, long senderId, UUID clientMessageId);

	ChatMessage save(
			long conversationId,
			long senderId,
			boolean operator,
			UUID clientMessageId,
			String body);
}
