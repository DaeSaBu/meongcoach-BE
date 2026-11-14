package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.application.required.ChatMessageRepository;
import com.daesabu.meongcoach.support.domain.ChatAttachment;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.ChatMessageCommand;
import com.daesabu.meongcoach.support.domain.exception.InvalidChatAttachmentException;
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
	private final StoredMediaVerifier storedMedia;

	@Transactional
	public ChatConversation open(Long userId) {
		conversations.insertIfAbsent(userId, java.time.LocalDateTime.now());
		return finder.findMemberConversation(userId);
	}

	@Transactional
	public ChatMessage send(Long userId, long conversationId, ChatMessageRequest request) {
		var conversation = finder.findLockedConversation(userId, conversationId);
		var previous = messages.findByConversationIdAndSenderIdAndClientMessageId(
				conversationId, userId, request.clientMessageId());
		if (previous.isPresent()) {
			return previous.get();
		}
		boolean operator = finder.isOperator();
		ChatAttachment attachment = null;
		if (request.attachment() != null) {
			attachment = request.attachment().toAttachment();
			if (!storedMedia.isOwnedObjectKey(attachment.getType().getScope(), userId, attachment.getObjectKey())) {
				throw new InvalidChatAttachmentException();
			}
		}
		var command = new ChatMessageCommand(conversationId, userId, operator, request.clientMessageId(), request.body(), attachment);
		var message = ChatMessage.create(command);
		messages.save(message);
		conversation.recordMessage();
		events.publishEvent(message.sentEvent(conversation.getMemberId()));
		return message;
	}
}
