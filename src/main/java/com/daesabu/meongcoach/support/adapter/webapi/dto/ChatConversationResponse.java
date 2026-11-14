package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatTimestampConverter;
import java.time.Instant;

public record ChatConversationResponse(long id, long memberId, Instant updatedAt) {

	public static ChatConversationResponse from(ChatConversation conversation) {
		return new ChatConversationResponse(conversation.getId(), conversation.getMemberId(), ChatTimestampConverter.toInstant(conversation.getUpdatedAt()));
	}
}
