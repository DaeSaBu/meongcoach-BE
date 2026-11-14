package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatPushRepository;
import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ChatModifyService implements ChatRegister {
	private final ChatRepository repository;
	private final ChatFinder finder;
	private final RegisteredUserChecker users;
	private final ChatPushRepository pushJobs;

	@Transactional
	public ChatConversation open(Long userId) {
		if (users.findRole(userId).isEmpty()) {
			throw new ChatAccessDeniedException();
		}
		return repository.open(userId);
	}

	@Transactional
	public ChatMessage send(Long userId, long conversationId, ChatMessageRequest request) {
		finder.verifyAccess(userId, conversationId);
		repository.lock(conversationId);
		var previous = repository.find(conversationId, userId, request.clientMessageId());
		if (previous.isPresent()) {
			return previous.get();
		}
		boolean operator = finder.isOperator(userId);
		String body = request.body().strip();
		ChatMessage message =
				repository.save(conversationId, userId, operator, request.clientMessageId(), body);
		if (operator && userId != conversationId) {
			pushJobs.enqueue(message.id(), conversationId);
		}
		return message;
	}
}
