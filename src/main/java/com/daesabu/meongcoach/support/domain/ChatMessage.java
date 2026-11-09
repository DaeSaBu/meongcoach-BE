package com.daesabu.meongcoach.support.domain;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
		long id,
		long conversationId,
		long senderId,
		boolean operator,
		UUID clientMessageId,
		String body,
		Instant createdAt) {}
