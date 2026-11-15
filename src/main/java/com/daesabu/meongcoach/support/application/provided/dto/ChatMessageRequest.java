package com.daesabu.meongcoach.support.application.provided.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatMessageRequest(
		@NotNull UUID clientMessageId, @Size(max = 2000) String body, @Valid ChatAttachmentRequest attachment) {
	public ChatMessageRequest(UUID clientMessageId, String body) {
		this(clientMessageId, body, null);
	}
}
