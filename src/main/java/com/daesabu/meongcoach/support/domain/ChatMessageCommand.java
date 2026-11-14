package com.daesabu.meongcoach.support.domain;

import java.util.UUID;

public record ChatMessageCommand(
		long conversationId, long senderId, boolean operator, UUID clientMessageId, String body, ChatAttachment attachment) {
	public ChatMessageCommand(long conversationId, long senderId, boolean operator, UUID clientMessageId, String body) {
		this(conversationId, senderId, operator, clientMessageId, body, null);
	}
}
