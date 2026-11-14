package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.util.List;

public interface ChatFinder {
	List<ChatConversation> conversations(long before);

	List<ChatMessage> latestMessages(long conversationId);

	List<ChatMessage> newMessages(long conversationId, long after);

	List<ChatMessage> previousMessages(long conversationId, long before);

	boolean isOperator(Long userId);

	void verifyExists(long conversationId);
}
