package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import java.time.Instant;

public record ChatConversationResponse(long id, Instant updatedAt) {

	public static ChatConversationResponse from(ChatConversation conversation) {
		return new ChatConversationResponse(conversation.getId(), conversation.getUpdatedAt());
	}
}
