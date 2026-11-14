package com.daesabu.meongcoach.support.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record ChatMessage(
		long id,
		long conversationId,
		long senderId,
		boolean operator,
		UUID clientMessageId,
		String body,
		Instant createdAt) {

	public Optional<ChatReplySentEvent> replySentEvent() {
		if (!operator || senderId == conversationId) {
			return Optional.empty();
		}
		var event = new ChatReplySentEvent(id, conversationId);
		return Optional.of(event);
	}
}
