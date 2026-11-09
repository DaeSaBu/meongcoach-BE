package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.domain.ChatAccess;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;

import java.util.List;

public interface ChatFinder {
	List<ChatConversation> conversations(Long userId, long before);

	List<ChatMessage> messages(Long userId, long conversationId, long after, long before);

	boolean isSocketAuthorized(ChatAccess access);

	boolean isOperator(Long userId);

	void verifyAccess(Long userId, long conversationId);
}
