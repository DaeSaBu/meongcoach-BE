package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import jakarta.validation.Valid;

public interface ChatRegister {
	ChatConversation open(Long userId);

	ChatMessage send(Long userId, long conversationId, @Valid ChatMessageRequest request);
}
