package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.ChatMessageCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ChatModifyService implements ChatRegister {
	private final ChatConversationRepository conversations;
	private final ChatMessageRepository messages;
	private final ChatFinder finder;
	private final ApplicationEventPublisher events;

	@Transactional
	public ChatConversation open(Long userId) {
		conversations.insertIfAbsent(userId);
		return finder.findConversation(userId);
	}

	@Transactional
	public ChatMessage send(Long userId, long conversationId, ChatMessageRequest request) {
		var conversation = finder.findLockedConversation(conversationId);
		var previous = messages.findByConversationIdAndSenderIdAndClientMessageId(
				conversationId, userId, request.clientMessageId());
		if (previous.isPresent()) {
			return previous.get();
		}
		boolean operator = finder.isOperator();
		String body = request.body().strip();
		var command = new ChatMessageCommand(conversationId, userId, operator, request.clientMessageId(), body);
		var message = ChatMessage.create(command);
		conversation.recordMessage();
		messages.save(message);
		events.publishEvent(message.sentEvent());
		return message;
	}
}
