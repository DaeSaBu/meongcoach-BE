package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatCapabilitiesResponse;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatConversationResponse;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatMessageResponse;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
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
			@RequestParam(defaultValue = "0") @PositiveOrZero long before,
			@RequestParam(required = false) Instant beforeUpdatedAt) {
		return finder.conversations(before, beforeUpdatedAt).stream()
				.map(ChatConversationResponse::from)
				.toList();
	}

	@GetMapping("/{id}/messages")
	public List<ChatMessageResponse> latestMessages(@PathVariable @Positive long id) {
		return finder.latestMessages(id).stream().map(ChatMessageResponse::from).toList();
	}

	@GetMapping("/{id}/messages/new")
	public List<ChatMessageResponse> newMessages(
			@PathVariable @Positive long id, @RequestParam @PositiveOrZero long after) {
		return finder.newMessages(id, after).stream().map(ChatMessageResponse::from).toList();
	}

	@GetMapping("/{id}/messages/previous")
	public List<ChatMessageResponse> previousMessages(
			@PathVariable @Positive long id, @RequestParam @Positive long before) {
		return finder.previousMessages(id, before).stream().map(ChatMessageResponse::from).toList();
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
