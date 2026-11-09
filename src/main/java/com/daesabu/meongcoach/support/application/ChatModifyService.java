package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatPushRepository;
import com.daesabu.meongcoach.support.application.required.ChatRepository;
import com.daesabu.meongcoach.support.domain.ChatAccess;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.time.Clock;
import java.util.UUID;
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
	private final Clock clock;

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
		repository.notifyChanged(conversationId);
		if (operator && userId != conversationId) {
			pushJobs.enqueue(message.id(), conversationId);
		}
		return message;
	}

	@Transactional
	public UUID issueTicket(Long userId) {
		if (users.findRole(userId).isEmpty()) {
			throw new ChatAccessDeniedException();
		}
		boolean operator = finder.isOperator(userId);
		ChatAccess access = new ChatAccess(userId, operator, clock.instant().plusSeconds(30));
		return repository.issueTicket(access);
	}

	@Transactional
	public ChatAccess consumeTicket(UUID ticket) {
		ChatAccess access =
				repository.consumeTicket(ticket).orElseThrow(ChatAccessDeniedException::new);
		if (!access.expiresAt().isAfter(clock.instant())
				|| users.findRole(access.userId()).isEmpty()) {
			throw new ChatAccessDeniedException();
		}
		boolean operator = finder.isOperator(access.userId());
		return new ChatAccess(access.userId(), operator, access.expiresAt());
	}
}
