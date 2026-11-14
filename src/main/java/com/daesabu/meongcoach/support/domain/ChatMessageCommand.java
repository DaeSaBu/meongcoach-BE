package com.daesabu.meongcoach.support.domain;

import java.util.UUID;

public record ChatMessageCommand(
		long conversationId, long senderId, boolean operator, UUID clientMessageId, String body) {
}
