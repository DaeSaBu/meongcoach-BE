package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatCapabilitiesResponse;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatConversationResponse;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatMessageResponse;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.ChatMessage;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
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
	public ChatCapabilitiesResponse capabilities() {
		boolean operator = finder.isOperator();
		return new ChatCapabilitiesResponse(operator);
	}

	@PostMapping("/me")
	public ChatConversationResponse open(@CurrentUserId Long userId) {
		var conversation = register.open(userId);
		return ChatConversationResponse.from(conversation);
	}

	@GetMapping
	public List<ChatConversationResponse> conversations(
			@RequestParam(defaultValue = "0") @PositiveOrZero long before) {
		return finder.conversations(before).stream()
				.map(ChatConversationResponse::from)
				.toList();
	}

	@GetMapping("/{id}/messages")
	public List<ChatMessageResponse> messages(
			@PathVariable @Positive long id,
			@RequestParam(defaultValue = "-1") @Min(-1) long after,
			@RequestParam(defaultValue = "0") @PositiveOrZero long before) {
		return findMessages(id, after, before).stream()
				.map(ChatMessageResponse::from)
				.toList();
	}

	private List<ChatMessage> findMessages(long id, long after, long before) {
		if (after >= 0 && before > 0) {
			throw new ChatCursorInvalidException();
		}
		if (after >= 0) {
			return finder.newMessages(id, after);
		}
		if (before > 0) {
			return finder.previousMessages(id, before);
		}
		return finder.latestMessages(id);
	}

	@PostMapping("/{id}/messages")
	public ChatMessageResponse send(
			@CurrentUserId Long userId,
			@PathVariable @Positive long id,
			@Valid @RequestBody ChatMessageRequest request) {
		var message = register.send(userId, id, request);
		return ChatMessageResponse.from(message);
	}

}
