package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.util.List;

public interface ChatFinder {
	List<ChatConversation> conversations(Long userId, long before);

	List<ChatMessage> latestMessages(Long userId, long conversationId);

	List<ChatMessage> newMessages(Long userId, long conversationId, long after);

	List<ChatMessage> previousMessages(Long userId, long conversationId, long before);

	boolean isOperator(Long userId);

	void verifyAccess(Long userId, long conversationId);
}
