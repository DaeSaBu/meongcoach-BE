package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.time.Instant;
import java.util.List;

public interface ChatFinder {
	ChatConversation findConversation(long conversationId);

	ChatConversation findLockedConversation(long conversationId);

	List<ChatConversation> conversations(long before, Instant beforeUpdatedAt);

	List<ChatMessage> latestMessages(long conversationId);

	List<ChatMessage> newMessages(long conversationId, long after);

	List<ChatMessage> previousMessages(long conversationId, long before);

	boolean isOperator();

	void verifyExists(long conversationId);
}
