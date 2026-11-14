package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import java.time.LocalDateTime;

public record ChatConversationResponse(long id, long memberId, LocalDateTime updatedAt) {

	public static ChatConversationResponse from(ChatConversation conversation) {
		return new ChatConversationResponse(conversation.getId(), conversation.getMemberId(), conversation.getLastMessageAt());
	}
}
