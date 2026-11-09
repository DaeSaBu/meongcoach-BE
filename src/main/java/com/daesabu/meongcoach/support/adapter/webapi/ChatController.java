package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/conversations")
@RequiredArgsConstructor
public class ChatController {
	private final ChatRegister register;
	private final ChatFinder finder;

	@GetMapping("/capabilities")
	public CapabilitiesResponse capabilities(@CurrentUserId Long userId) {
		boolean operator = finder.isOperator(userId);
		return new CapabilitiesResponse(operator);
	}

	public record ConversationResponse(long id, java.time.Instant updatedAt) {
		static ConversationResponse from(ChatConversation conversation) {
			return new ConversationResponse(conversation.id(), conversation.updatedAt());
		}
	}

	public record MessageResponse(
			long id,
			long conversationId,
			long senderId,
			boolean operator,
			UUID clientMessageId,
			String body,
			java.time.Instant createdAt) {
		static MessageResponse from(ChatMessage message) {
			return new MessageResponse(
					message.id(),
					message.conversationId(),
					message.senderId(),
					message.operator(),
					message.clientMessageId(),
					message.body(),
					message.createdAt());
		}
	}

	public record CapabilitiesResponse(boolean operator) {}

	@PostMapping("/me")
	public ConversationResponse open(@CurrentUserId Long userId) {
		var conversation = register.open(userId);
		return ConversationResponse.from(conversation);
	}

	@GetMapping
	public List<ConversationResponse> conversations(
			@CurrentUserId Long userId,
			@RequestParam(defaultValue = "0") @PositiveOrZero long before) {
		return finder.conversations(userId, before).stream()
				.map(ConversationResponse::from)
				.toList();
	}

	@GetMapping("/{id}/messages")
	public List<MessageResponse> messages(
			@CurrentUserId Long userId,
			@PathVariable @Positive long id,
			@RequestParam(defaultValue = "-1") @Min(-1) long after,
			@RequestParam(defaultValue = "0") @PositiveOrZero long before) {
		return finder.messages(userId, id, after, before).stream()
				.map(MessageResponse::from)
				.toList();
	}

	@PostMapping("/{id}/messages")
	public MessageResponse send(
			@CurrentUserId Long userId,
			@PathVariable @Positive long id,
			@Valid @RequestBody ChatMessageRequest request) {
		var message = register.send(userId, id, request);
		return MessageResponse.from(message);
	}

	@PostMapping("/socket-ticket")
	public SocketTicketResponse ticket(@CurrentUserId Long userId) {
		UUID ticket = register.issueTicket(userId);
		return new SocketTicketResponse(ticket);
	}

	public record SocketTicketResponse(UUID ticket) {}
}
