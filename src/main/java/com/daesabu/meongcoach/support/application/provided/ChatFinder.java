package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatFinder {
	ChatConversation findMemberConversation(long memberId);

	ChatConversation findConversation(Long userId, long conversationId);

	ChatConversation findLockedConversation(Long userId, long conversationId);

	List<ChatConversation> conversations(long before, LocalDateTime beforeUpdatedAt);

	List<ChatMessage> latestMessages(Long userId, long conversationId);

	List<ChatMessage> newMessages(Long userId, long conversationId, long after);

	List<ChatMessage> previousMessages(Long userId, long conversationId, long before);

	boolean isOperator();
}
