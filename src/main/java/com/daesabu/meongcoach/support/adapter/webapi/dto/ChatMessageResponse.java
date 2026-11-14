package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.support.domain.ChatMessage;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
		long id,
		long conversationId,
		long senderId,
		boolean operator,
		UUID clientMessageId,
		String body,
		LocalDateTime createdAt) {

	public static ChatMessageResponse from(ChatMessage message) {
		return new ChatMessageResponse(
				message.getId(),
				message.getConversationId(),
				message.getSenderId(),
				message.isOperator(),
				message.getClientMessageId(),
				message.getBody(),
				message.getCreatedAt());
	}
}
