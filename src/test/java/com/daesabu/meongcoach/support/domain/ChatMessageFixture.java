package com.daesabu.meongcoach.support.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class ChatMessageFixture {
	public static ChatMessage create(
			long id, long conversationId, long senderId, boolean operator,
			UUID clientMessageId, String body, Instant createdAt) {
		var command = new ChatMessageCommand(conversationId, senderId, operator, clientMessageId, body);
		var message = ChatMessage.create(command);
		ReflectionTestUtils.setField(message, "id", id);
		ReflectionTestUtils.setField(message, "createdAt", createdAt);
		return message;
	}
}
